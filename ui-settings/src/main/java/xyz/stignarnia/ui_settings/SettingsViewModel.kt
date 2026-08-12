package xyz.stignarnia.ui_settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import xyz.stignarnia.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.ui_base.viewmodel.ChannelsDelegate
import xyz.stignarnia.ui_base.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.ui_settings.views.SettingsFiltersView.SettingsFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor() :
  ViewModel(),
  ChannelsDelegate by DefaultChannelsDelegate() {

    private val filterState = MutableStateFlow<SettingsFilter?>(null)

    fun setFilter(filter: SettingsFilter?) {
      filterState.value = filter
    }

    val uiState = filterState
      .map { SettingsUiState(filter = it) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
        initialValue = SettingsUiState(),
      )
  }
