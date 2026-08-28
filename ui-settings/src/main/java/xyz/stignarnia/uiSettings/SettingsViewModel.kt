package xyz.stignarnia.uiSettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.uiBase.viewmodel.ChannelsDelegate
import xyz.stignarnia.uiBase.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.uiSettings.views.SettingsFiltersView.SettingsFilter
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
  @Inject
  constructor() :
  ViewModel(),
    ChannelsDelegate by DefaultChannelsDelegate() {
    private val filterState = MutableStateFlow<SettingsFilter?>(null)

    fun setFilter(filter: SettingsFilter?) {
      filterState.value = filter
    }

    val uiState =
      filterState
        .map { SettingsUiState(filter = it) }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
          initialValue = SettingsUiState(),
        )
  }
