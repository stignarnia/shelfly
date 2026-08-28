package xyz.stignarnia.uiMovie.sections.related

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.uiBase.utilities.extensions.findReplace
import xyz.stignarnia.uiBase.utilities.extensions.rethrowCancellation
import xyz.stignarnia.uiBase.viewmodel.ChannelsDelegate
import xyz.stignarnia.uiBase.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiMovie.cases.MovieDetailsMyMoviesCase
import xyz.stignarnia.uiMovie.sections.related.cases.MovieDetailsRelatedCase
import xyz.stignarnia.uiMovie.sections.related.recycler.RelatedListItem
import javax.inject.Inject

@HiltViewModel
class MovieDetailsRelatedViewModel
  @Inject
  constructor(
    private val relatedCase: MovieDetailsRelatedCase,
    private val myMoviesCase: MovieDetailsMyMoviesCase,
    private val imagesProvider: MovieImagesProvider,
  ) : ViewModel(),
    ChannelsDelegate by DefaultChannelsDelegate() {
    private lateinit var movie: Movie

    private val loadingState = MutableStateFlow(true)
    private val relatedItemsState = MutableStateFlow<List<RelatedListItem>?>(null)

    fun initRelatedMovies(movie: Movie) {
      if (this::movie.isInitialized) return
      this.movie = movie
      loadRelatedMovies()
    }

    fun loadRelatedMovies() {
      if (!this::movie.isInitialized) return
      viewModelScope.launch {
        try {
          val (myMovies, watchlistMovies) = myMoviesCase.getAllIds()
          val related =
            relatedCase.loadRelatedMovies(movie).map {
              val image = imagesProvider.findCachedImage(it, ImageType.POSTER)
              RelatedListItem(
                movie = it,
                image = image,
                isFollowed = it.tmdbId in myMovies,
                isWatchlist = it.tmdbId in watchlistMovies,
              )
            }
          relatedItemsState.value = related
        } catch (error: Throwable) {
          relatedItemsState.value = emptyList()
          Timber.e(error)
          rethrowCancellation(error)
        } finally {
          loadingState.value = false
        }
      }
      Timber.d("Loading related movies...")
    }

    fun loadMissingImage(
      item: RelatedListItem,
      force: Boolean,
    ) {
      fun updateItem(new: RelatedListItem) {
        val currentItems = uiState.value.relatedMovies?.toMutableList()
        currentItems?.findReplace(new) { it isSameAs new }
        relatedItemsState.value = currentItems
      }

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

    val uiState =
      combine(
        loadingState,
        relatedItemsState,
      ) { s1, s2 ->
        MovieDetailsRelatedUiState(
          isLoading = s1,
          relatedMovies = s2,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
        initialValue = MovieDetailsRelatedUiState(),
      )
  }
