package xyz.stignarnia.ui_movie.sections.ratings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import xyz.stignarnia.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.ui_base.utilities.extensions.rethrowCancellation
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.Ratings
import xyz.stignarnia.ui_movie.sections.ratings.cases.MovieDetailsRatingCase
import xyz.stignarnia.ui_movie.sections.ratings.cases.MovieDetailsRatingSpoilersCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MovieDetailsRatingsViewModel @Inject constructor(
  private val ratingsCase: MovieDetailsRatingCase,
  private val ratingsSpoilersCase: MovieDetailsRatingSpoilersCase,
) : ViewModel() {

  private var loadedImdbId: String? = null

  private val movieState = MutableStateFlow<Movie?>(null)
  private val ratingsState = MutableStateFlow<Ratings?>(null)
  private val isRefreshingRatingsState = MutableStateFlow(false)

  fun loadRatings(movie: Movie) {
    val imdbId = movie.ids.imdb.id
    if (loadedImdbId != null) return
    loadedImdbId = imdbId

    viewModelScope.launch {
      movieState.value = movie

      val externalRatings = Ratings(
        tmdb = Ratings.Value(String.format(Locale.ENGLISH, "%.1f", movie.rating), false),
        imdb = Ratings.Value(null, true),
        metascore = Ratings.Value(null, true),
        rottenTomatoes = Ratings.Value(null, true),
      )

      ratingsState.value = ratingsSpoilersCase.hideSpoilerRatings(movie, externalRatings)

      // Every external rating comes from OMDb, so with no key there is nothing
      // to fetch. Flag it rather than leaving three slots blank for no reason.
      if (!ratingsCase.hasOmdbApiKey()) {
        val settled = externalRatings.settled().copy(isOmdbKeyMissing = true)
        ratingsState.value = ratingsSpoilersCase.hideSpoilerRatings(movie, settled)
        return@launch
      }

      // External ratings are looked up by IMDb id. Without one there is nothing
      // to ask OMDb for, so settle rather than spinning on a request that could
      // only fail.
      if (imdbId.isBlank()) {
        ratingsState.value = ratingsSpoilersCase.hideSpoilerRatings(movie, externalRatings.settled())
        return@launch
      }

      try {
        val ratings = ratingsCase.loadExternalRatings(movie)
        ratingsState.value = ratingsSpoilersCase.hideSpoilerRatings(movie, ratings)
      } catch (error: Throwable) {
        ratingsState.value = ratingsSpoilersCase.hideSpoilerRatings(movie, externalRatings.settled())
        rethrowCancellation(error)
      }
    }
  }

  /**
   * Clears the loading flags so a failed lookup renders as absent rather than
   * spinning forever.
   */
  private fun Ratings.settled() =
    copy(
      imdb = Ratings.Value(imdb?.value, false),
      metascore = Ratings.Value(metascore?.value, false),
      rottenTomatoes = Ratings.Value(rottenTomatoes?.value, false),
    )

  fun refreshRatings() {
    val movie = movieState.value
    val ratings = ratingsState.value
    viewModelScope.launch {
      if (movie != null && ratings != null) {
        isRefreshingRatingsState.value = true
        ratingsState.value = ratingsSpoilersCase.hideSpoilerRatings(movie, ratings)
      }
    }
  }

  val uiState = combine(
    ratingsState,
    movieState,
    isRefreshingRatingsState,
  ) { s1, s2, s3 ->
    MovieDetailsRatingsUiState(
      ratings = s1,
      movie = s2,
      isRefreshingRatings = s3,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
    initialValue = MovieDetailsRatingsUiState(),
  )
}
