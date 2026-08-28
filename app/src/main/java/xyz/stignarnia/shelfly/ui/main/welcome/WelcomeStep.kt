package xyz.stignarnia.shelfly.ui.main.welcome

import androidx.annotation.StringRes
import xyz.stignarnia.shelfly.R
import xyz.stignarnia.uiSettings.helpers.AppLanguage

/**
 * A single screen of the welcome flow.
 *
 * Steps are plain data.
 * [xyz.stignarnia.shelfly.ui.main.cases.MainWelcomeCase] decides which ones are due and in which order, [xyz.stignarnia.shelfly.ui.main.MainViewModel] walks the resulting queue and [xyz.stignarnia.shelfly.ui.views.welcome.WelcomeView] renders whichever one is current.
 * Adding a screen means adding a variant here and a condition in the case, not another dialog with its own callback chain.
 */
sealed interface WelcomeStep {
  /** Stable key used to record that this step has been completed. */
  val id: String

  @get:StringRes val primaryButton: Int

  /** Second forward action, for steps that are a choice rather than an acknowledgement. */
  @get:StringRes val secondaryButton: Int?
    get() = null

  /**
   * Offered whenever the device language differs from the one the app is running in, not only on first run, and placed before everything else so that the rest of the flow follows the language just picked.
   *
   * The secondary label is built from [current] rather than being a fixed resource, since what the user is declining is "keep the language I am already using", which is not always English.
   */
  data class Language(
    val suggested: AppLanguage,
    val current: AppLanguage,
  ) : WelcomeStep {
    override val id = ID
    override val primaryButton = R.string.textChangeLanguage

    companion object {
      const val ID = "LANGUAGE"
    }
  }

  data object Disclaimer : WelcomeStep {
    const val ID = "DISCLAIMER"
    override val id = ID
    override val primaryButton = R.string.textDisclaimerConfirmText
  }

  /**
   * A key the user pastes in.
   * Both services share a screen shape, and differ only in their copy and in whether the flow can move on without an answer.
   */
  sealed interface ApiKey : WelcomeStep {
    @get:StringRes val title: Int

    @get:StringRes val message: Int

    @get:StringRes val hint: Int

    /**
     * Without a TMDB key there is no catalog at all, so that step is queued whenever no key is stored - on first run and equally on any later launch after the key has been cleared.
     */
    data object Tmdb : ApiKey {
      const val ID = "API_KEY_TMDB"
      override val id = ID
      override val title = R.string.textOnboardingApiKeyTitle
      override val message = R.string.textOnboardingApiKeyMessage
      override val hint = R.string.textOnboardingApiKeyHint
      override val primaryButton = R.string.textContinue
    }

    /**
     * Ratings only, so this one is skippable - and skipping is remembered, since the key stays unset and the condition alone would ask again every launch.
     */
    data object Omdb : ApiKey {
      const val ID = "API_KEY_OMDB"
      override val id = ID
      override val title = R.string.textOnboardingOmdbKeyTitle
      override val message = R.string.textOnboardingOmdbKeyMessage
      override val hint = R.string.textOnboardingOmdbKeyHint
      override val primaryButton = R.string.textContinue
      override val secondaryButton = R.string.textSkip
    }
  }

  /**
   * API 33 and up only, where notifications need a runtime grant.
   * First run switches episode notifications off precisely because the permission has not been asked for yet, so granting here turns them back on.
   */
  data object Notifications : WelcomeStep {
    const val ID = "NOTIFICATIONS"
    override val id = ID
    override val primaryButton = R.string.textOnboardingNotificationsEnable
    override val secondaryButton = R.string.textNotNow
  }

  /**
   * Points at the existing backup screen rather than asking for a URL and credentials inline: the setup form already exists, and duplicating it here would mean two places to keep correct.
   */
  data object WebDavSync : WelcomeStep {
    const val ID = "WEBDAV_SYNC"
    override val id = ID
    override val primaryButton = R.string.textOnboardingSyncSetUp
    override val secondaryButton = R.string.textNotNow
  }

  data class WhatsNew(
    val version: String,
    val notes: String,
  ) : WelcomeStep {
    override val id = ID
    override val primaryButton = R.string.textClose

    companion object {
      const val ID = "WHATS_NEW"
    }
  }
}
