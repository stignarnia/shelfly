package xyz.stignarnia.shelfly.ui.main

import xyz.stignarnia.shelfly.ui.main.welcome.WelcomeState
import xyz.stignarnia.shelfly.utilities.deeplink.DeepLinkBundle
import xyz.stignarnia.ui_base.utilities.events.Event

data class MainUiState(
  val isLoading: Boolean = false,
  val welcome: WelcomeState? = null,
  val showDiscover: Event<Boolean>? = null,
  val requestNotifications: Event<Boolean>? = null,
  val openSettings: Event<Boolean>? = null,
  val showMask: Boolean = false,
  val openLink: Event<DeepLinkBundle>? = null,
)
