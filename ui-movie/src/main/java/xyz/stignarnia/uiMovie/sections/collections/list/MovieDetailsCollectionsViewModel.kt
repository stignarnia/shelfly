package xyz.stignarnia.uiMovie.sections.collections.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.stignarnia.repository.movies.MovieCollectionsRepository.Source
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.uiBase.utilities.extensions.rethrowCancellation
import xyz.stignarnia.uiBase.viewmodel.ChannelsDelegate
import xyz.stignarnia.uiBase.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.MovieCollection
import xyz.stignarnia.uiMovie.MovieDetailsEvent.OpenCollectionSheet
import xyz.stignarnia.uiMovie.sections.collections.list.cases.MovieDetailsCollectionsCase
import javax.inject.Inject

@HiltViewModel
class MovieDetailsCollectionsViewModel
  @Inject
  constructor(
    private val collectionsCase: MovieDetailsCollectionsCase,
  ) : ViewModel(),
    ChannelsDelegate by DefaultChannelsDelegate() {
    private lateinit var movie: Movie
    private var lastOpenedCollection: IdTmdb? = null

    private val loadingState = MutableStateFlow(true)
    private val movieCollectionState = MutableStateFlow<Pair<List<MovieCollection>, Source>?>(null)

    fun loadCollections(movie: Movie) {
      if (this::movie.isInitialized) {
        return
      }
      this.movie = movie

      viewModelScope.launch {
        try {
          movieCollectionState.value = collectionsCase.loadMovieCollections(movie)
        } catch (error: Throwable) {
          Timber.e(error)
          rethrowCancellation(error)
        } finally {
          loadingState.value = false
        }
      }
      Timber.d("Loading movie collections...")
    }

    fun loadCollection(collection: MovieCollection) {
      viewModelScope.launch {
        eventChannel.send(OpenCollectionSheet(movie, collection))
      }
    }

    fun loadLastOpenedCollection() {
      lastOpenedCollection?.let { id ->
        val collection = movieCollectionState.value?.first?.find { it.id == id }
        collection?.let {
          loadCollection(collection)
          lastOpenedCollection = null
        }
      }
    }

    fun saveLastOpenedCollection(collectionId: IdTmdb) {
      lastOpenedCollection = collectionId
    }

    val uiState =
      combine(
        loadingState,
        movieCollectionState,
      ) { s1, s2 ->
        MovieDetailsCollectionsUiState(
          isLoading = s1,
          collections = s2,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
        initialValue = MovieDetailsCollectionsUiState(),
      )
  }
