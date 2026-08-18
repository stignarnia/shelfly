package xyz.stignarnia.ui_settings.sections.general

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import xyz.stignarnia.common.Config
import xyz.stignarnia.ui_base.common.AppCountry
import xyz.stignarnia.ui_base.dates.AppDateFormat
import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.ui_base.utilities.extensions.combine
import xyz.stignarnia.ui_model.ProgressDateSelectionType
import xyz.stignarnia.ui_model.ProgressNextEpisodeType
import xyz.stignarnia.ui_model.Settings
import xyz.stignarnia.ui_settings.helpers.AppLanguage
import xyz.stignarnia.ui_settings.helpers.AppTheme
import xyz.stignarnia.ui_settings.sections.general.cases.SettingsGeneralMainCase
import xyz.stignarnia.ui_settings.sections.general.cases.SettingsGeneralStreamingsCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsGeneralViewModel @Inject constructor(
  private val mainCase: SettingsGeneralMainCase,
  private val streamingsCase: SettingsGeneralStreamingsCase,
) : ViewModel() {

  private val settingsState = MutableStateFlow<Settings?>(null)
  private val languageState = MutableStateFlow(AppLanguage.ENGLISH)
  private val countryState = MutableStateFlow<AppCountry?>(null)
  private val dateFormatState = MutableStateFlow<AppDateFormat?>(null)
  private val moviesEnabledState = MutableStateFlow(true)
  private val streamingsEnabledState = MutableStateFlow(true)
  private val themeState = MutableStateFlow(AppTheme.DARK)
  private val amoledState = MutableStateFlow(false)
  private val recreateActivityState = MutableStateFlow<Event<Boolean>?>(null)
  private val restartAppState = MutableStateFlow(false)
  private val progressTypeState = MutableStateFlow<ProgressNextEpisodeType?>(null)
  private val progressDateSelectionState = MutableStateFlow<ProgressDateSelectionType?>(null)
  private val progressUpcomingDaysState = MutableStateFlow<Long?>(null)
  private val tabletsColumnsState = MutableStateFlow(Config.DEFAULT_LISTS_GRID_SPAN)

  fun loadSettings() {
    viewModelScope.launch {
      refreshSettings()
    }
  }

  private suspend fun refreshSettings(restartApp: Boolean = false) {
    settingsState.value = mainCase.getSettings()
    languageState.value = mainCase.getLanguage()
    themeState.value = mainCase.getTheme()
    amoledState.value = mainCase.isAmoled()
    countryState.value = mainCase.getCountry()
    dateFormatState.value = mainCase.getDateFormat()
    moviesEnabledState.value = mainCase.isMoviesEnabled()
    streamingsEnabledState.value = mainCase.isStreamingsEnabled()
    progressTypeState.value = mainCase.getProgressType()
    progressDateSelectionState.value = mainCase.getDateSelectionType()
    progressUpcomingDaysState.value = mainCase.getProgressUpcomingDays()
    tabletsColumnsState.value = mainCase.getTabletsColumns()
    restartAppState.value = restartApp
  }

  fun setRecentShowsAmount(amount: Int) {
    viewModelScope.launch {
      mainCase.setRecentShowsAmount(amount)
      refreshSettings()
    }
  }

  fun enableSpecialSeasons(enable: Boolean) {
    viewModelScope.launch {
      mainCase.enableSpecialSeasons(enable)
      refreshSettings()
    }
  }

  fun enableMovies(enable: Boolean) {
    viewModelScope.launch {
      mainCase.enableMovies(enable)
      delay(300)
      refreshSettings(restartApp = true)
    }
  }

  fun enableStreamings(enable: Boolean) {
    viewModelScope.launch {
      mainCase.enableStreamings(enable)
      refreshSettings()
    }
  }

  fun setLanguage(language: AppLanguage) {
    viewModelScope.launch {
      mainCase.setLanguage(language)
      val locales = LocaleListCompat.forLanguageTags(language.code)
      AppCompatDelegate.setApplicationLocales(locales)
    }
  }

  fun setTheme(theme: AppTheme) {
    viewModelScope.launch {
      mainCase.setTheme(theme)
      // The night mode goes on here rather than in BaseActivity so that the Activity being built next already has it, and AppCompat has no reason to re-apply its theme mid-creation and flatten the overlays.
      AppCompatDelegate.setDefaultNightMode(theme.nightMode)
      // Asked for unconditionally: AppCompat only recreates when the resolved configuration changes, so dark to follow-system on an already dark phone would otherwise change nothing.
      recreateActivityState.value = Event(true)
      refreshSettings()
    }
  }

  fun setAmoled(enabled: Boolean) {
    viewModelScope.launch {
      mainCase.setAmoled(enabled)
      recreateActivityState.value = Event(true)
      refreshSettings()
    }
  }

  fun setTabletColumns(columns: Int) {
    viewModelScope.launch {
      mainCase.setTabletsColumns(columns)
      refreshSettings()
    }
  }

  fun setCountry(country: AppCountry) {
    viewModelScope.launch {
      mainCase.setCountry(country)
      streamingsCase.deleteCache()
      refreshSettings()
    }
  }

  fun setProgressType(type: ProgressNextEpisodeType) {
    viewModelScope.launch {
      mainCase.setProgressType(type)
      refreshSettings()
    }
  }

  fun setDateSelectionType(type: ProgressDateSelectionType) {
    viewModelScope.launch {
      mainCase.setDateSelectionType(type)
      refreshSettings()
    }
  }

  fun setProgressUpcomingDays(days: Long) {
    viewModelScope.launch {
      mainCase.setProgressUpcomingDays(days)
      refreshSettings()
    }
  }

  fun setDateFormat(
    format: AppDateFormat,
    context: Context,
  ) {
    viewModelScope.launch {
      mainCase.setDateFormat(format, context)
      refreshSettings()
    }
  }

  val uiState = combine(
    settingsState,
    themeState,
    languageState,
    countryState,
    dateFormatState,
    moviesEnabledState,
    streamingsEnabledState,
    progressTypeState,
    restartAppState,
    progressUpcomingDaysState,
    tabletsColumnsState,
    progressDateSelectionState,
    amoledState,
    recreateActivityState,
  ) { s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14 ->
    SettingsGeneralUiState(
      settings = s1,
      theme = s2,
      language = s3,
      country = s4,
      dateFormat = s5,
      moviesEnabled = s6,
      streamingsEnabled = s7,
      progressNextType = s8,
      restartApp = s9,
      progressUpcomingDays = s10,
      tabletColumns = s11,
      progressDateSelectionType = s12,
      amoled = s13,
      recreateActivity = s14,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
    initialValue = SettingsGeneralUiState(),
  )
}
