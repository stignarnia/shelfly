package xyz.stignarnia.ui_my_shows.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import xyz.stignarnia.common.Config
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.ui_base.common.ListViewMode
import xyz.stignarnia.ui_base.events.EventsManager
import xyz.stignarnia.ui_base.events.ReloadData
import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.ui_base.utilities.extensions.findReplace
import xyz.stignarnia.ui_base.viewmodel.ChannelsDelegate
import xyz.stignarnia.ui_base.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortType
import xyz.stignarnia.ui_my_shows.common.recycler.CollectionListItem
import xyz.stignarnia.ui_my_shows.common.recycler.CollectionListItem.ShowItem
import xyz.stignarnia.ui_my_shows.main.FollowedShowsUiState
import xyz.stignarnia.ui_my_shows.watchlist.cases.WatchlistFiltersCase
import xyz.stignarnia.ui_my_shows.watchlist.cases.WatchlistLoadShowsCase
import xyz.stignarnia.ui_my_shows.watchlist.cases.WatchlistSortOrderCase
import xyz.stignarnia.ui_my_shows.watchlist.cases.WatchlistTranslationsCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import xyz.stignarnia.ui_base.events.Event as EventSync

@HiltViewModel
class WatchlistViewModel @Inject constructor(
  private val sortOrderCase: WatchlistSortOrderCase,
  private val filtersCase: WatchlistFiltersCase,
  private val loadShowsCase: WatchlistLoadShowsCase,
  private val translationsCase: WatchlistTranslationsCase,
  private val imagesProvider: ShowImagesProvider,
  private val eventsManager: EventsManager,
) : ViewModel(),
  ChannelsDelegate by DefaultChannelsDelegate() {

  private var loadItemsJob: Job? = null

  private val itemsState = MutableStateFlow<List<CollectionListItem>>(emptyList())
  private val viewModeState = MutableStateFlow(ListViewMode.LIST_NORMAL)
  private val sortOrderState = MutableStateFlow<Event<Pair<SortOrder, SortType>>?>(null)
  private val scrollState = MutableStateFlow<Event<Boolean>?>(null)

  private var searchQuery: String? = null

  init {
    viewModelScope.launch { eventsManager.events.collect { onEvent(it) } }
  }

  fun onParentState(state: FollowedShowsUiState) {
    when {
      this.searchQuery != state.searchQuery -> {
        this.searchQuery = state.searchQuery
        loadShows(resetScroll = state.searchQuery.isNullOrBlank())
      }
    }
  }

  fun loadShows(resetScroll: Boolean = false) {
    loadItemsJob?.cancel()
    loadItemsJob = viewModelScope.launch {
      itemsState.value = loadShowsCase.loadShows(searchQuery ?: "")
      scrollState.value = Event(resetScroll)
    }
  }

  fun setSortOrder(
    sortOrder: SortOrder,
    sortType: SortType,
  ) {
    viewModelScope.launch {
      sortOrderCase.setSortOrder(sortOrder, sortType)
      loadShows(resetScroll = true)
    }
  }

  fun toggleUpcomingFilter() {
    viewModelScope.launch {
      filtersCase.toggleUpcomingFilter()
      loadShows(resetScroll = true)
    }
  }

  fun loadMissingImage(
    item: CollectionListItem,
    force: Boolean,
  ) {
    check(item is ShowItem)
    viewModelScope.launch {
      updateItem(item.copy(isLoading = true))
      try {
        val image = imagesProvider.loadRemoteImage(item.show, item.image.type, force)
        updateItem(item.copy(isLoading = false, image = image))
      } catch (t: Throwable) {
        updateItem(item.copy(isLoading = false, image = Image.createUnavailable(item.image.type)))
      }
    }
  }

  fun loadMissingTranslation(item: CollectionListItem) {
    check(item is ShowItem)
    if (item.translation != null || translationsCase.getLanguage() == Config.DEFAULT_LANGUAGE) return
    viewModelScope.launch {
      try {
        val translation = translationsCase.loadTranslation(item.show, false)
        updateItem(item.copy(translation = translation))
      } catch (error: Throwable) {
        Timber.e(error)
      }
    }
  }

  private fun updateItem(new: CollectionListItem) {
    val currentItems = uiState.value.items.toMutableList()
    currentItems.findReplace(new) { it.isSameAs(new) }
    itemsState.value = currentItems
  }

  private fun onEvent(event: EventSync) =
    when (event) {
      is ReloadData -> loadShows()
      else -> Unit
    }

  val uiState = combine(
    itemsState,
    sortOrderState,
    scrollState,
    viewModeState,
  ) { s1, s2, s3, s4 ->
    WatchlistUiState(
      items = s1,
      sortOrder = s2,
      resetScroll = s3,
      viewMode = s4,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
    initialValue = WatchlistUiState(),
  )
}
