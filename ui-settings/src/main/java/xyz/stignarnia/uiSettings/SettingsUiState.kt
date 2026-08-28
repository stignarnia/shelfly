package xyz.stignarnia.uiSettings

import xyz.stignarnia.uiSettings.views.SettingsFiltersView

data class SettingsUiState(
  val filter: SettingsFiltersView.SettingsFilter? = null,
)
