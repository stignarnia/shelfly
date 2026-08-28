package xyz.stignarnia.shelfly.ui.main.welcome

import xyz.stignarnia.uiSettings.helpers.AppLanguage

/**
 * Where the welcome flow currently is.
 *
 * This is state rather than a one shot event: it is rebuilt from the view model on every render, so a configuration change - including the activity restart that applying a new locale causes - resumes the flow exactly where it was.
 */
data class WelcomeState(
  val step: WelcomeStep,
  val index: Int,
  val total: Int,
  /**
   * The language the flow itself must be drawn in, which is the app's stored language and not necessarily the one Android would pick.
   * On a true first run no per app locale has been applied yet, so resources would otherwise resolve to the device language - and asking an Italian speaking phone, in Italian, whether it would like to switch to Italian makes no sense.
   */
  val displayLanguage: AppLanguage,
  val apiKeyDraft: String = "",
) {
  val isBackEnabled: Boolean
    get() = index > 0

  val isPrimaryEnabled: Boolean
    get() = step !is WelcomeStep.ApiKey || apiKeyDraft.isNotBlank()
}
