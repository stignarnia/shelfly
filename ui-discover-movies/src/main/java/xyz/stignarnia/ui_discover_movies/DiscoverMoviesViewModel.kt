package xyz.stignarnia.ui_discover_movies

import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.Companion.PRIVATE
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.data_remote.apikey.ApiKeyProvider
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_base.utilities.events.MessageEvent
import xyz.stignarnia.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.ui_base.utilities.extensions.findReplace
import xyz.stignarnia.ui_base.utilities.extensions.rethrowCancellation
import xyz.stignarnia.ui_base.viewmodel.ChannelsDelegate
import xyz.stignarnia.ui_base.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.ui_discover_movies.cases.DiscoverFiltersCase
import xyz.stignarnia.ui_discover_movies.cases.DiscoverMoviesCase
import xyz.stignarnia.ui_discover_movies.recycler.DiscoverMovieListItem
import xyz.stignarnia.ui_model.DiscoverFilters
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.ImageFamily.MOVIE
import xyz.stignarnia.ui_model.ImageSource.TMDB
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class DiscoverMoviesViewModel @Inject constructor(
  private val moviesCase: DiscoverMoviesCase,
  private val filtersCase: DiscoverFiltersCase,
  private val imagesProvider: MovieImagesProvider,
  private val apiKeyProvider: ApiKeyProvider,
) : ViewModel(),
  ChannelsDelegate by DefaultChannelsDelegate() {

  private val itemsState = MutableStateFlow<List<DiscoverMovieListItem>?>(null)
  private val loadingState = MutableStateFlow(false)
  private val filtersState = MutableStateFlow<DiscoverFilters?>(null)
  private val scrollState = MutableStateFlow(Event(false))

  private var initialFilters: DiscoverFilters? = null

  init {
    viewModelScope.launch {
      initialFilters = filtersCase.loadFilters()
    }
  }

  fun loadMovies(
    pullToRefresh: Boolean = false,
    resetScroll: Boolean = false,
    skipCache: Boolean = false,
    instantProgress: Boolean = false,
  ) {
    loadingState.value = true

    loadingState.value = pullToRefresh

    viewModelScope.launch {
      val progressJob = launch {
        delay(if (pullToRefresh || instantProgress) 0 else 750)
        loadingState.value = true
      }

      try {
        val filters = filtersCase.loadFilters()
        filtersState.value = filters

        if (!pullToRefresh && !skipCache) {
          val movies = moviesCase.loadCachedMovies(filters)
          itemsState.value = movies
          scrollState.value = Event(resetScroll)
        }

        // Nothing to fetch without a key, and trying anyway would leave the screen showing a failure the user cannot act on until they have finished entering one.
        if (apiKeyProvider.hasTmdbApiKey() && (pullToRefresh || skipCache || !moviesCase.isCacheValid())) {
          val movies = moviesCase.loadRemoteMovies(filters)
          itemsState.value = emptyList()
          delay(50) // Added to avoid long scrolling to top
          itemsState.value = movies
          scrollState.value = Event(resetScroll)
          initialFilters = filters
        }

        if (pullToRefresh) {
        }
      } catch (error: Throwable) {
        onError(error)
      } finally {
        loadingState.value = false
        progressJob.cancel()
      }
    }
  }

  fun loadMissingImage(
    item: DiscoverMovieListItem,
    force: Boolean,
  ) {
    fun updateItem(newItem: DiscoverMovieListItem) {
      itemsState.update { value ->
        value?.toMutableList()?.apply {
          findReplace(newItem) { it.isSameAs(newItem) }
        }
      }
      scrollState.update { Event(false) }
    }

    viewModelScope.launch {
      val loadingJob = launch {
        delay(750)
        updateItem(item.copy(isLoading = true))
      }
      try {
        val image = imagesProvider.loadRemoteImage(item.movie, item.image.type, force)
        updateItem(item.copy(isLoading = false, image = image))
      } catch (t: Throwable) {
        updateItem(item.copy(isLoading = false, image = Image.createUnavailable(item.image.type, MOVIE, TMDB)))
        rethrowCancellation(t)
      } finally {
        loadingJob.cancel()
      }
    }
  }

  fun toggleCollection() {
    viewModelScope.launch {
      filtersCase.toggleCollection()
      loadMovies(resetScroll = true, skipCache = true, instantProgress = true)
    }
  }

  private suspend fun onError(error: Throwable) {
    if (error !is CancellationException) {
      messageChannel.send(MessageEvent.Error(R.string.errorCouldNotLoadDiscover))
      Timber.e(error)
    }
    rethrowCancellation(error)
  }

  override fun onCleared() {
    filtersCase.revertFilters(
      initialFilters = initialFilters,
      currentFilters = filtersState.value,
    )
  }

  val uiState = combine(
    itemsState,
    loadingState,
    filtersState,
    scrollState,
  ) { s1, s2, s3, s4 ->
    DiscoverMoviesUiState(
      items = s1,
      isLoading = s2,
      filters = s3,
      resetScroll = s4,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
    initialValue = DiscoverMoviesUiState(),
  )
}
