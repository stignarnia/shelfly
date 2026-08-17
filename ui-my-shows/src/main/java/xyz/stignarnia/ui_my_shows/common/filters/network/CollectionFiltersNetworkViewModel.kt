package xyz.stignarnia.ui_my_shows.common.filters.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.repository.shows.ShowsRepository
import xyz.stignarnia.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.ui_base.viewmodel.ChannelsDelegate
import xyz.stignarnia.ui_base.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.ui_my_shows.common.filters.CollectionFiltersOrigin
import xyz.stignarnia.ui_my_shows.common.filters.CollectionFiltersOrigin.HIDDEN_SHOWS
import xyz.stignarnia.ui_my_shows.common.filters.CollectionFiltersOrigin.MY_SHOWS
import xyz.stignarnia.ui_my_shows.common.filters.CollectionFiltersOrigin.WATCHLIST_SHOWS
import xyz.stignarnia.ui_my_shows.common.filters.CollectionFiltersUiEvent.ApplyFilters
import xyz.stignarnia.ui_my_shows.common.filters.CollectionFiltersUiEvent.CloseFilters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
internal class CollectionFiltersNetworkViewModel @Inject constructor(
  private val settingsRepository: SettingsRepository,
  private val showsRepository: ShowsRepository,
  private val dispatchers: CoroutineDispatchers,
) : ViewModel(),
  ChannelsDelegate by DefaultChannelsDelegate() {

  private val availableState = MutableStateFlow<List<String>?>(null)
  private val selectedState = MutableStateFlow(emptyList<String>())
  private val loadingState = MutableStateFlow(false)

  private lateinit var origin: CollectionFiltersOrigin

  fun loadData(origin: CollectionFiltersOrigin) {
    this.origin = origin
    viewModelScope.launch {
      selectedState.value = selectedNetworks()
      loadingState.value = true
      // The offered networks are the ones the collection actually holds rather than a fixed list, so a filter is never offered that matches nothing and no broadcaster is missing because it was never enumerated.
      availableState.value = withContext(dispatchers.IO) {
        val shows = when (origin) {
          MY_SHOWS -> showsRepository.myShows.loadAll()
          WATCHLIST_SHOWS -> showsRepository.watchlistShows.loadAll()
          HIDDEN_SHOWS -> showsRepository.hiddenShows.loadAll()
        }
        shows
          .map { it.network }
          .filter { it.isNotBlank() }
          .distinct()
          .sorted()
      }
      loadingState.value = false
    }
  }

  fun saveNetworks(networks: List<String>) {
    viewModelScope.launch {
      if (networks.toSet() == selectedState.value.toSet()) {
        eventChannel.send(CloseFilters)
        return@launch
      }
      when (origin) {
        MY_SHOWS -> settingsRepository.filters.myShowsNetworks = networks
        WATCHLIST_SHOWS -> settingsRepository.filters.watchlistShowsNetworks = networks
        HIDDEN_SHOWS -> settingsRepository.filters.hiddenShowsNetworks = networks
      }
      eventChannel.send(ApplyFilters)
    }
  }

  private fun selectedNetworks() =
    when (origin) {
      MY_SHOWS -> settingsRepository.filters.myShowsNetworks
      WATCHLIST_SHOWS -> settingsRepository.filters.watchlistShowsNetworks
      HIDDEN_SHOWS -> settingsRepository.filters.hiddenShowsNetworks
    }

  val uiState = combine(
    availableState,
    selectedState,
    loadingState,
  ) { s1, s2, s3 ->
    CollectionFiltersNetworkUiState(
      available = s1,
      selected = s2,
      isLoading = s3,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
    initialValue = CollectionFiltersNetworkUiState(),
  )
}
