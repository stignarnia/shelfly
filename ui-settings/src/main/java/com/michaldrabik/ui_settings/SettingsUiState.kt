package com.michaldrabik.ui_settings

import com.michaldrabik.ui_settings.views.SettingsFiltersView

data class SettingsUiState(
  val filter: SettingsFiltersView.SettingsFilter? = null,
)
