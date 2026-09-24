package xyz.stignarnia.uiSettings.helpers

import android.app.LocaleManager
import android.content.Context
import android.content.SharedPreferences
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import xyz.stignarnia.common.Config
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.repository.utilities.BooleanPreference
import xyz.stignarnia.uiBase.common.WidgetsProvider
import xyz.stignarnia.uiBase.utilities.AndroidVersion
import javax.inject.Inject
import javax.inject.Named

/**
 * The stored language drives both the UI and the content translations, so every change of it goes through here.
 *
 * The system keeps a per-app language of its own on API 33+, which the user can change outside the app.
 * The two are kept in step in both directions: a choice made in the system settings is adopted by [systemOverride], and the stored language is handed to the system by [pin] when the system has none.
 */
class AppLanguageSwitcher
  @Inject
  constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    @Named("localePreferences") localePreferences: SharedPreferences,
  ) {
    companion object {
      private const val APP_LOCALE_SET = "APP_LOCALE_SET"
      private const val FOLLOWS_SYSTEM_LANGUAGE = "FOLLOWS_SYSTEM_LANGUAGE"
    }

    /** Whether a per-app language was set in the system the last time it was looked at. */
    private var wasAppLocaleSet by BooleanPreference(localePreferences, APP_LOCALE_SET)

    /** Whether the user cleared the per-app language in the system settings, asking for the app to follow the device language. */
    private var followsSystemLanguage by BooleanPreference(localePreferences, FOLLOWS_SYSTEM_LANGUAGE)

    /**
     * The language the system settings ask this app to run in, when it disagrees with the stored one.
     *
     * Clearing the per-app language back to "System default" there is a choice as well: the app should then follow the device language.
     * A per-app language that was never set is not a choice, though - it is a fresh install or a restore - and is left to [pin].
     * The two look the same from here, so which one it is comes from whether a per-app language was set when last seen.
     *
     * Null when there is nothing to adopt, including a per-app language the app has no translation for.
     * A device language without a translation resolves to English, since that is what the resources fall back to.
     *
     * LocaleManagerCompat asks the system directly; AppCompatDelegate.getApplicationLocales returns nothing until an Activity exists.
     */
    fun systemOverride(context: Context): AppLanguage? {
      if (!AndroidVersion.isAtLeastAndroid13) return null
      val appLocale = LocaleManagerCompat.getApplicationLocales(context)[0]
      val language =
        if (appLocale != null) {
          wasAppLocaleSet = true
          followsSystemLanguage = false
          AppLanguage.entries.firstOrNull { it.code == appLocale.language } ?: return null
        } else {
          if (wasAppLocaleSet) followsSystemLanguage = true
          wasAppLocaleSet = false
          if (!followsSystemLanguage) return null
          AppLanguage.fromLocales(LocaleManagerCompat.getSystemLocales(context)) ?: AppLanguage.ENGLISH
        }
      return language.takeIf { it.code != settingsRepository.language }
    }

    /**
     * Hands the stored language to the system, for when the system does not have it - a fresh install, or the stored language restored from a backup, which carries the app's preferences but not the system's per-app setting.
     * Left alone when the user asked to follow the device language, since pinning would undo that.
     *
     * On API 33+ this goes to LocaleManager directly: AppCompatDelegate reaches it only through an Activity, and from the Application there is none yet, so it would drop the call.
     */
    fun pin(context: Context) {
      val language = settingsRepository.language
      if (!AndroidVersion.isAtLeastAndroid13) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
        return
      }
      if (followsSystemLanguage || !LocaleManagerCompat.getApplicationLocales(context).isEmpty) return
      context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(language)
      wasAppLocaleSet = true
    }

    /**
     * Stores [language] and clears the translations of every other one, which also lets the new language's be fetched straight away rather than after the sync cooldown.
     * The widgets are repainted once that is done: they are drawn in the app's language and only read cached translations and posters, so nothing else would bring them up to date.
     */
    suspend fun switchTo(language: AppLanguage) {
      settingsRepository.run {
        this.language = language.code
        val unused =
          AppLanguage.entries
            .filter { it.code != Config.DEFAULT_LANGUAGE && it != language }
            .map { it.code }
        clearUnusedTranslations(unused)
        clearLanguageLogs()
      }
      (context as WidgetsProvider).requestAllWidgetsUpdate()
    }
  }
