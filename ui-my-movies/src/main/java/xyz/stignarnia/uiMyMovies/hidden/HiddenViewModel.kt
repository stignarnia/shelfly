package xyz.stignarnia.uiMyMovies.hidden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.stignarnia.common.Config
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiBase.common.ListViewMode
import xyz.stignarnia.uiBase.events.EventsManager
import xyz.stignarnia.uiBase.events.ReloadData
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.uiBase.utilities.extensions.findReplace
import xyz.stignarnia.uiBase.viewmodel.ChannelsDelegate
import xyz.stignarnia.uiBase.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiMyMovies.common.recycler.CollectionListItem
import xyz.stignarnia.uiMyMovies.hidden.cases.HiddenLoadMoviesCase
import xyz.stignarnia.uiMyMovies.hidden.cases.HiddenSortOrderCase
import xyz.stignarnia.uiMyMovies.main.FollowedMoviesUiState
import javax.inject.Inject
import xyz.stignarnia.uiBase.events.Event as EventSync

@HiltViewModel
class HiddenViewModel
  @Inject
  constructor(
    private val sortOrderCase: HiddenSortOrderCase,
    private val loadMoviesCase: HiddenLoadMoviesCase,
    private val settingsRepository: SettingsRepository,
    private val imagesProvider: MovieImagesProvider,
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
      viewModelScope.launch {
        eventsManager.events.collect { onEvent(it) }
      }
    }

    fun onParentState(state: FollowedMoviesUiState) {
      when {
        this.searchQuery != state.searchQuery -> {
          this.searchQuery = state.searchQuery
          loadMovies(resetScroll = state.searchQuery.isNullOrBlank())
        }
      }
    }

    fun loadMovies(resetScroll: Boolean = false) {
      loadItemsJob?.cancel()
      loadItemsJob =
        viewModelScope.launch {
          itemsState.value = loadMoviesCase.loadMovies(searchQuery ?: "")
          scrollState.value = Event(resetScroll)
        }
    }

    fun setSortOrder(
      sortOrder: SortOrder,
      sortType: SortType,
    ) {
      viewModelScope.launch {
        sortOrderCase.setSortOrder(sortOrder, sortType)
        loadMovies(resetScroll = true)
      }
    }

    fun loadMissingImage(
      item: CollectionListItem,
      force: Boolean,
    ) {
      check(item is CollectionListItem.MovieItem)
      viewModelScope.launch {
        updateItem(item.copy(isLoading = true))
        try {
          val image = imagesProvider.loadRemoteImage(item.movie, item.image.type, force)
          updateItem(item.copy(isLoading = false, image = image))
        } catch (t: Throwable) {
          updateItem(item.copy(isLoading = false, image = Image.createUnavailable(item.image.type)))
        }
      }
    }

    fun loadMissingTranslation(item: CollectionListItem) {
      check(item is CollectionListItem.MovieItem)
      if (item.translation != null || settingsRepository.language == Config.DEFAULT_LANGUAGE) return
      viewModelScope.launch {
        try {
          val translation = loadMoviesCase.loadTranslation(item.movie, false)
          updateItem(item.copy(translation = translation))
        } catch (error: Throwable) {
          Timber.e(error)
        }
      }
    }

    private fun updateItem(new: CollectionListItem) {
      val currentItems = uiState.value.items.toMutableList()
      currentItems.findReplace(new) { it isSameAs (new) }
      itemsState.value = currentItems
    }

    private fun onEvent(event: EventSync) =
      when (event) {
        is ReloadData -> loadMovies()
        else -> Unit
      }

    val uiState =
      combine(
        itemsState,
        sortOrderState,
        scrollState,
        viewModeState,
      ) { s1, s2, s3, s4 ->
        HiddenUiState(
          items = s1,
          sortOrder = s2,
          resetScroll = s3,
          viewMode = s4,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
        initialValue = HiddenUiState(),
      )
  }
