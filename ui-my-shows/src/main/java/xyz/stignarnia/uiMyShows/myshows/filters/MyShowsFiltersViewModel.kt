package xyz.stignarnia.uiMyShows.myshows.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.uiBase.viewmodel.ChannelsDelegate
import xyz.stignarnia.uiBase.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.uiModel.MyShowsSection
import xyz.stignarnia.uiMyShows.myshows.filters.MyShowsFiltersUiEvent.ApplyFilters
import xyz.stignarnia.uiMyShows.myshows.filters.MyShowsFiltersUiEvent.CloseFilters
import javax.inject.Inject

@HiltViewModel
internal class MyShowsFiltersViewModel
  @Inject
  constructor(
    private val settingsRepository: SettingsRepository,
  ) : ViewModel(),
    ChannelsDelegate by DefaultChannelsDelegate() {
    private val sectionState = MutableStateFlow<MyShowsSection?>(null)
    private val loadingState = MutableStateFlow(false)

    init {
      viewModelScope.launch {
        sectionState.value = settingsRepository.filters.myShowsType
      }
    }

    fun applySectionType(sectionType: MyShowsSection) {
      viewModelScope.launch {
        if (sectionType == sectionState.value) {
          eventChannel.send(CloseFilters)
          return@launch
        }
        settingsRepository.filters.myShowsType = sectionType
        eventChannel.send(ApplyFilters)
      }
    }

    val uiState =
      combine(
        sectionState,
        loadingState,
      ) { s1, s2 ->
        MyShowsFiltersUiState(
          sectionType = s1,
          isLoading = s2,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
        initialValue = MyShowsFiltersUiState(),
      )
  }
