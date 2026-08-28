package xyz.stignarnia.uiMyMovies.mymovies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.stignarnia.common.Config.DEFAULT_LANGUAGE
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
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.ImageType.POSTER
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.MyMoviesSection.ALL
import xyz.stignarnia.uiModel.MyMoviesSection.RECENTS
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiModel.SpoilersSettings
import xyz.stignarnia.uiModel.UserRating
import xyz.stignarnia.uiMyMovies.main.FollowedMoviesUiState
import xyz.stignarnia.uiMyMovies.mymovies.cases.MyMoviesLoadCase
import xyz.stignarnia.uiMyMovies.mymovies.cases.MyMoviesRatingsCase
import xyz.stignarnia.uiMyMovies.mymovies.cases.MyMoviesSortingCase
import xyz.stignarnia.uiMyMovies.mymovies.recycler.MyMoviesItem
import xyz.stignarnia.uiMyMovies.mymovies.recycler.MyMoviesItem.Type
import xyz.stignarnia.uiMyMovies.mymovies.recycler.MyMoviesItem.Type.ALL_MOVIES_ITEM
import xyz.stignarnia.uiMyMovies.mymovies.recycler.MyMoviesItem.Type.RECENT_MOVIES
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import xyz.stignarnia.uiBase.events.Event as EventSync

@HiltViewModel
class MyMoviesViewModel
  @Inject
  constructor(
    private val loadMoviesCase: MyMoviesLoadCase,
    private val ratingsCase: MyMoviesRatingsCase,
    private val sortingCase: MyMoviesSortingCase,
    private val settingsRepository: SettingsRepository,
    private val eventsManager: EventsManager,
  ) : ViewModel(),
    ChannelsDelegate by DefaultChannelsDelegate() {
    private var loadItemsJob: Job? = null

    private val itemsState = MutableStateFlow<List<MyMoviesItem>?>(null)
    private val itemsUpdateState = MutableStateFlow<Event<Boolean>?>(null)
    private val viewModeState = MutableStateFlow(ListViewMode.LIST_NORMAL)
    private val showEmptyViewState = MutableStateFlow(false)

    private var searchQuery: String? = null

    init {
      viewModelScope.launch { eventsManager.events.collect { onEvent(it) } }
    }

    fun onParentState(state: FollowedMoviesUiState) {
      when {
        this.searchQuery != state.searchQuery -> {
          this.searchQuery = state.searchQuery
          loadMovies(notifyListsUpdate = state.searchQuery.isNullOrBlank())
        }
      }
    }

    fun loadMovies(notifyListsUpdate: Boolean = false) {
      loadItemsJob?.cancel()
      loadItemsJob =
        viewModelScope.launch {
          val settings = loadMoviesCase.loadSettings()
          val dateFormat = loadMoviesCase.loadDateFormat()
          val ratings = ratingsCase.loadRatings()
          val sortOrder = sortingCase.loadSortOrder()
          val genresFilter = settingsRepository.filters.myMoviesGenres
          val spoilers = settingsRepository.spoilers.getAll()

          val movies =
            loadMoviesCase
              .loadAll()
              .map {
                toListItemAsync(
                  itemType = ALL_MOVIES_ITEM,
                  movie = it,
                  dateFormat = dateFormat,
                  type = POSTER,
                  userRating = ratings[it.ids.tmdb],
                  sortOrder = sortOrder.first,
                  spoilers = spoilers,
                )
              }.awaitAll()

          val allMovies =
            loadMoviesCase.filterSectionMovies(
              allMovies = movies,
              sortOrder = sortOrder,
              genres = genresFilter.map { it.slug },
              searchQuery = searchQuery,
            )
          val recentMovies =
            if (settings.myRecentsAmount > 0) {
              loadMoviesCase
                .loadRecentMovies()
                .map {
                  toListItemAsync(
                    itemType = RECENT_MOVIES,
                    movie = it,
                    dateFormat = dateFormat,
                    type = ImageType.FANART,
                    userRating = ratings[it.ids.tmdb],
                    sortOrder = sortOrder.first,
                    spoilers = spoilers,
                  )
                }.awaitAll()
            } else {
              emptyList()
            }

          val isNotSearching = searchQuery.isNullOrBlank()
          val hasAnyFilters = genresFilter.isNotEmpty()
          val listItems = mutableListOf<MyMoviesItem>()
          listItems.run {
            if (isNotSearching && recentMovies.isNotEmpty()) {
              add(MyMoviesItem.createHeader(RECENTS, recentMovies.count(), null, null))
              add(MyMoviesItem.createRecentsSection(recentMovies))
            }
            if (allMovies.isNotEmpty() || hasAnyFilters) {
              add(
                MyMoviesItem.createHeader(
                  section = ALL,
                  itemCount = allMovies.count(),
                  sortOrder = sortOrder,
                  genres = genresFilter,
                ),
              )
              addAll(allMovies)
            }
          }

          itemsState.value = listItems
          itemsUpdateState.value = Event(notifyListsUpdate)
          showEmptyViewState.value = movies.isEmpty()
        }
    }

    fun setSortOrder(
      order: SortOrder,
      type: SortType,
    ) {
      viewModelScope.launch {
        sortingCase.setSortOrder(order, type)
        loadMovies(notifyListsUpdate = true)
      }
    }

    fun loadMissingImage(
      item: MyMoviesItem,
      force: Boolean,
    ) {
      viewModelScope.launch {
        updateItem(item.copy(isLoading = true))
        try {
          val image = loadMoviesCase.loadMissingImage(item.movie, item.image.type, force)
          updateItem(item.copy(isLoading = false, image = image))
        } catch (t: Throwable) {
          updateItem(item.copy(isLoading = false, image = Image.createUnavailable(item.image.type)))
        }
      }
    }

    fun loadMissingTranslation(item: MyMoviesItem) {
      if (item.translation != null || settingsRepository.language == DEFAULT_LANGUAGE) return
      viewModelScope.launch {
        try {
          val translation = loadMoviesCase.loadTranslation(item.movie, false)
          updateItem(item.copy(translation = translation))
        } catch (error: Throwable) {
          Timber.e(error)
        }
      }
    }

    private fun updateItem(new: MyMoviesItem) {
      val items = uiState.value.items?.toMutableList()
      items?.findReplace(new) { it isSameAs new }
      itemsState.value = items
    }

    private fun CoroutineScope.toListItemAsync(
      itemType: Type,
      movie: Movie,
      dateFormat: DateTimeFormatter,
      type: ImageType = POSTER,
      userRating: UserRating?,
      sortOrder: SortOrder?,
      spoilers: SpoilersSettings,
    ) = async {
      val image = loadMoviesCase.findCachedImage(movie, type)
      val translation = loadMoviesCase.loadTranslation(movie, true)
      MyMoviesItem(
        type = itemType,
        header = null,
        recentsSection = null,
        movie = movie,
        image = image,
        isLoading = false,
        translation = translation,
        userRating = userRating?.rating,
        dateFormat = dateFormat,
        sortOrder = sortOrder,
        spoilers =
          MyMoviesItem.Spoilers(
            isSpoilerHidden = spoilers.isMyMoviesHidden,
            isSpoilerRatingsHidden = spoilers.isMyMoviesRatingsHidden,
            isSpoilerTapToReveal = spoilers.isTapToReveal,
          ),
      )
    }

    private fun onEvent(event: EventSync) =
      when (event) {
        is ReloadData -> loadMovies()
        else -> Unit
      }

    val uiState =
      combine(
        itemsState,
        itemsUpdateState,
        viewModeState,
        showEmptyViewState,
      ) { s1, s2, s3, s4 ->
        MyMoviesUiState(
          items = s1,
          resetScroll = s2,
          viewMode = s3,
          showEmptyView = s4,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
        initialValue = MyMoviesUiState(),
      )
  }
