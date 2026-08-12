package xyz.stignarnia.ui_settings

import xyz.stignarnia.ui_settings.views.SettingsFiltersView

data class SettingsUiState(
  val filter: SettingsFiltersView.SettingsFilter? = null,
)
