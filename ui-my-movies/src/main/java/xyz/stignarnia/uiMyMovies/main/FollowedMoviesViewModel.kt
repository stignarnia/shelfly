package xyz.stignarnia.uiMyMovies.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.stignarnia.uiBase.events.EventsManager
import xyz.stignarnia.uiBase.events.ReloadData
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import javax.inject.Inject

@HiltViewModel
class FollowedMoviesViewModel
  @Inject
  constructor(
    private val eventsManager: EventsManager,
  ) : ViewModel() {
    private val searchQueryState = MutableStateFlow<String?>(null)

    fun onSearchQuery(searchQuery: String?) {
      searchQueryState.value = searchQuery
    }

    fun refreshData() {
      viewModelScope.launch {
        eventsManager.sendEvent(ReloadData)
      }
    }

    val uiState =
      searchQueryState
        .map { FollowedMoviesUiState(searchQuery = it) }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
          initialValue = FollowedMoviesUiState(),
        )
  }
