package xyz.stignarnia.uiDiscoverMovies.filters.feed

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
import xyz.stignarnia.uiDiscoverMovies.filters.feed.DiscoverMoviesFiltersFeedUiEvent.ApplyFilters
import xyz.stignarnia.uiDiscoverMovies.filters.feed.DiscoverMoviesFiltersFeedUiEvent.CloseFilters
import xyz.stignarnia.uiModel.DiscoverFeed
import javax.inject.Inject

@HiltViewModel
internal class DiscoverMoviesFiltersFeedViewModel
  @Inject
  constructor(
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
        feedOrderState.value = settingsRepository.filters.discoverMoviesFeed
      }
    }

    fun saveFeedOrder(feedOrder: DiscoverFeed) {
      viewModelScope.launch {
        if (feedOrder == feedOrderState.value) {
          eventChannel.send(CloseFilters)
          return@launch
        }
        settingsRepository.filters.discoverMoviesFeed = feedOrder
        eventChannel.send(ApplyFilters)
      }
    }

    val uiState =
      combine(
        feedOrderState,
        loadingState,
      ) { s1, s2 ->
        DiscoverMoviesFiltersFeedUiState(
          feedOrder = s1,
          isLoading = s2,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
        initialValue = DiscoverMoviesFiltersFeedUiState(),
      )
  }
