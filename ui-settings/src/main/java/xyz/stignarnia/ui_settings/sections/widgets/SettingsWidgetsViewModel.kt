package xyz.stignarnia.ui_settings.sections.widgets

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import xyz.stignarnia.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.ui_model.Settings
import xyz.stignarnia.ui_settings.sections.widgets.cases.SettingsWidgetsMainCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsWidgetsViewModel @Inject constructor(
  private val mainCase: SettingsWidgetsMainCase,
) : ViewModel() {

  private val settingsState = MutableStateFlow<Settings?>(null)

  fun loadSettings() {
    viewModelScope.launch {
      refreshSettings()
    }
  }

  fun enableWidgetsTitles(
    enable: Boolean,
    context: Context,
  ) {
    viewModelScope.launch {
      mainCase.enableWidgetsTitles(enable, context)
      refreshSettings()
    }
  }

  private suspend fun refreshSettings() {
    settingsState.value = mainCase.getSettings()
  }

  val uiState = settingsState
    .map { SettingsWidgetsUiState(settings = it) }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
      initialValue = SettingsWidgetsUiState(),
    )
}
