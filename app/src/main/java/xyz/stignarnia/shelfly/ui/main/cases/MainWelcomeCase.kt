package xyz.stignarnia.shelfly.ui.main.cases

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.dataRemote.apikey.ApiKeyProvider
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.repository.settings.SettingsWebDavRepository
import xyz.stignarnia.shelfly.BuildConfig
import xyz.stignarnia.shelfly.ui.main.welcome.WelcomeStep
import xyz.stignarnia.uiBase.utilities.AndroidVersion
import xyz.stignarnia.uiSettings.helpers.AppLanguage
import javax.inject.Inject
import javax.inject.Named

/**
 * Owns every "should this welcome screen appear" decision.
 *
 * All triggers are evaluated once, up front, so the screens that are due are known as a single ordered list instead of being discovered one callback at a time.
 * Completion is recorded when the user finishes a step, not when it is queued, so a flow abandoned halfway is resumed on the next launch.
 */
@ViewModelScoped
class MainWelcomeCase
  @Inject
  constructor(
    @param:ApplicationContext private val context: Context,
    private val initialsCase: MainInitialsCase,
    private val apiKeyProvider: ApiKeyProvider,
    private val settingsRepository: SettingsRepository,
    private val webDavRepository: SettingsWebDavRepository,
    @param:Named("miscPreferences") private val miscPreferences: SharedPreferences,
  ) {
    companion object {
      private const val KEY_COMPLETED_STEPS = "WELCOME_COMPLETED_STEPS"
      private const val KEY_LANGUAGE_SUGGESTED = "WELCOME_LANGUAGE_SUGGESTED"
      private const val RELEASE_NOTES_ASSET = "release_notes.txt"

      /**
       * Steps an install that predates the welcome flow must not be ambushed with on upgrade.
       * They are all optional, and a wall of new screens in place of the app is a poor way to announce them.
       */
      private val UPGRADE_SEED =
        setOf(
          WelcomeStep.Disclaimer.ID,
          WelcomeStep.ApiKey.Omdb.ID,
          WelcomeStep.Notifications.ID,
          WelcomeStep.WebDavSync.ID,
        )
    }

    fun buildQueue(isInitialRun: Boolean): List<WelcomeStep> {
      migrate(isInitialRun)
      val completed = completedSteps()

      return buildList {
        languageStep()?.let { add(it) }
        if (WelcomeStep.Disclaimer.ID !in completed) {
          add(WelcomeStep.Disclaimer)
        }
        if (!apiKeyProvider.hasTmdbApiKey()) {
          add(WelcomeStep.ApiKey.Tmdb)
        }
        if (!apiKeyProvider.hasOmdbApiKey() && WelcomeStep.ApiKey.Omdb.ID !in completed) {
          add(WelcomeStep.ApiKey.Omdb)
        }
        if (WelcomeStep.Notifications.ID !in completed && needsNotificationsPermission()) {
          add(WelcomeStep.Notifications)
        }
        if (WelcomeStep.WebDavSync.ID !in completed && webDavRepository.url.isBlank()) {
          add(WelcomeStep.WebDavSync)
        }
        if (initialsCase.showWhatsNew(isInitialRun)) {
          add(WelcomeStep.WhatsNew(BuildConfig.VERSION_NAME, readReleaseNotes()))
        }
      }
    }

    /**
     * Offered whenever the device is set to a language the app is not running in, on any launch rather than only the first - switching the phone to French is exactly when being asked about French is useful.
     *
     * A declined suggestion is remembered by language, not as a one off flag, so saying no settles that language for good while a later switch to a different one still asks.
     */
    private fun languageStep(): WelcomeStep.Language? {
      val suggested = initialsCase.detectSystemLanguage() ?: return null
      val current = currentLanguage()
      if (suggested == current || suggested.code == lastSuggestedLanguage()) {
        return null
      }
      return WelcomeStep.Language(suggested = suggested, current = current)
    }

    fun currentLanguage(): AppLanguage = AppLanguage.fromCode(settingsRepository.language)

    private fun needsNotificationsPermission(): Boolean {
      if (!AndroidVersion.isAtLeastAndroid13) return false
      val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
      return granted != PackageManager.PERMISSION_GRANTED
    }

    fun setStepCompleted(step: WelcomeStep) {
      when (step) {
        // Clears itself: the step is only queued while no key is stored.
        is WelcomeStep.ApiKey.Tmdb -> Unit

        // Recorded as the seen version stamp rather than as a step id.
        is WelcomeStep.WhatsNew -> initialsCase.setWhatsNewSeen()

        // Recorded by language, so that declining does not silence future changes.
        is WelcomeStep.Language -> setLastSuggestedLanguage(step.suggested)

        else -> markCompleted(step.id)
      }
    }

    fun setLanguage(language: AppLanguage) = initialsCase.setLanguage(language)

    fun setTmdbApiKey(key: String) = apiKeyProvider.setTmdbApiKey(key)

    fun setOmdbApiKey(key: String) = apiKeyProvider.setOmdbApiKey(key)

    /**
     * First run switches episode notifications off because the runtime permission has not been asked for yet, so a grant here has to switch them back on.
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
      val settings = settingsRepository.load()
      settingsRepository.update(settings.copy(episodesNotificationsEnabled = enabled))
    }

    /**
     * Installs that already passed the previous first run flow have no record of it, and must not be shown those screens again.
     * Seed one, once, the first time this build runs: the disclaimer counts as acknowledged, the newly added optional steps count as declined, and the language currently on the device counts as already asked about - only a change from here on should prompt.
     */
    private fun migrate(isInitialRun: Boolean) {
      if (miscPreferences.contains(KEY_COMPLETED_STEPS)) return
      if (isInitialRun) {
        miscPreferences.edit { putStringSet(KEY_COMPLETED_STEPS, emptySet()) }
        return
      }
      miscPreferences.edit {
        putStringSet(KEY_COMPLETED_STEPS, UPGRADE_SEED)
        initialsCase.detectSystemLanguage()?.let { putString(KEY_LANGUAGE_SUGGESTED, it.code) }
      }
    }

    private fun completedSteps(): Set<String> =
      miscPreferences.getStringSet(KEY_COMPLETED_STEPS, emptySet())
        ?: emptySet()

    private fun markCompleted(id: String) {
      miscPreferences.edit {
        putStringSet(KEY_COMPLETED_STEPS, completedSteps() + id)
      }
    }

    private fun lastSuggestedLanguage(): String? = miscPreferences.getString(KEY_LANGUAGE_SUGGESTED, null)

    private fun setLastSuggestedLanguage(language: AppLanguage) {
      miscPreferences.edit { putString(KEY_LANGUAGE_SUGGESTED, language.code) }
    }

    private fun readReleaseNotes(): String =
      context.assets
        .open(RELEASE_NOTES_ASSET)
        .bufferedReader()
        .use { it.readText() }
  }
