package xyz.stignarnia.ui_discover.filters.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.ui_base.viewmodel.ChannelsDelegate
import xyz.stignarnia.ui_base.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.ui_discover.filters.feed.DiscoverFiltersFeedUiEvent.ApplyFilters
import xyz.stignarnia.ui_discover.filters.feed.DiscoverFiltersFeedUiEvent.CloseFilters
import xyz.stignarnia.ui_model.DiscoverFeed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class DiscoverFiltersFeedViewModel @Inject constructor(
  private val settingsRepository: SettingsRepository,
) : ViewModel(),
  ChannelsDelegate by DefaultChannelsDelegate() {

  private val feedOrderState = MutableStateFlow<DiscoverFeed?>(null)
  private val loadingState = MutableStateFlow(false)

  init {
    loadFilters()
  }

  private fun loadFilters() {
    viewModelScope.launch {
      feedOrderState.value = settingsRepository.filters.discoverShowsFeed
    }
  }

  fun saveFeedOrder(feedOrder: DiscoverFeed) {
    viewModelScope.launch {
      if (feedOrder == feedOrderState.value) {
        eventChannel.send(CloseFilters)
        return@launch
      }
      settingsRepository.filters.discoverShowsFeed = feedOrder
      eventChannel.send(ApplyFilters)
    }
  }

  val uiState = combine(
    feedOrderState,
    loadingState,
  ) { s1, s2 ->
    DiscoverFiltersFeedUiState(
      feedOrder = s1,
      isLoading = s2,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
    initialValue = DiscoverFiltersFeedUiState(),
  )
}
