package xyz.stignarnia.ui_settings.sections.general

import xyz.stignarnia.common.Config
import xyz.stignarnia.ui_base.common.AppCountry
import xyz.stignarnia.ui_base.dates.AppDateFormat
import xyz.stignarnia.ui_model.ProgressDateSelectionType
import xyz.stignarnia.ui_model.ProgressNextEpisodeType
import xyz.stignarnia.ui_model.Settings
import xyz.stignarnia.ui_settings.helpers.AppLanguage
import xyz.stignarnia.ui_settings.helpers.AppTheme

data class SettingsGeneralUiState(
  val settings: Settings? = null,
  val language: AppLanguage = AppLanguage.ENGLISH,
  val theme: AppTheme = AppTheme.DARK,
  val country: AppCountry? = null,
  val dateFormat: AppDateFormat? = null,
  val moviesEnabled: Boolean = true,
  val newsEnabled: Boolean = false,
  val streamingsEnabled: Boolean = true,
  val restartApp: Boolean = false,
  val progressNextType: ProgressNextEpisodeType? = null,
  val progressDateSelectionType: ProgressDateSelectionType? = null,
  val progressUpcomingDays: Long? = null,
  val tabletColumns: Int = Config.DEFAULT_LISTS_GRID_SPAN,
)
