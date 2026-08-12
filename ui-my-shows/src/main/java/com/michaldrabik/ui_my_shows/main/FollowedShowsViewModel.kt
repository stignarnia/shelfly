package com.michaldrabik.ui_my_shows.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michaldrabik.ui_base.events.EventsManager
import com.michaldrabik.ui_base.events.ReloadData
import com.michaldrabik.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FollowedShowsViewModel @Inject constructor(
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

  val uiState = searchQueryState
    .map { FollowedShowsUiState(searchQuery = it) }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
      initialValue = FollowedShowsUiState(),
    )
}
