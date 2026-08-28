package xyz.stignarnia.uiSettings.sections.general

import xyz.stignarnia.common.Config
import xyz.stignarnia.uiBase.common.AppCountry
import xyz.stignarnia.uiBase.dates.AppDateFormat
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiModel.ProgressDateSelectionType
import xyz.stignarnia.uiModel.ProgressNextEpisodeType
import xyz.stignarnia.uiModel.Settings
import xyz.stignarnia.uiSettings.helpers.AppLanguage
import xyz.stignarnia.uiSettings.helpers.AppTheme

data class SettingsGeneralUiState(
  val settings: Settings? = null,
  val language: AppLanguage = AppLanguage.ENGLISH,
  val theme: AppTheme = AppTheme.DARK,
  val amoled: Boolean = false,
  val recreateActivity: Event<Boolean>? = null,
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
