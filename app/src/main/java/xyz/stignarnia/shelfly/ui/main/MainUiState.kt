package xyz.stignarnia.shelfly.ui.main

import xyz.stignarnia.shelfly.utilities.deeplink.DeepLinkBundle
import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_settings.helpers.AppLanguage

// TODO Split events into their Channel
data class MainUiState(
  val isLoading: Boolean = false,
  val isInitialRun: Event<Boolean>? = null,
  val showWhatsNew: Event<Boolean>? = null,
  val initialLanguage: Event<AppLanguage>? = null,
  val showMask: Boolean = false,
  val openLink: Event<DeepLinkBundle>? = null,
)
