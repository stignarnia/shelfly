package xyz.stignarnia.uiShow.sections.ratings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.uiBase.utilities.extensions.rethrowCancellation
import xyz.stignarnia.uiModel.Ratings
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiShow.sections.ratings.cases.ShowDetailsRatingCase
import xyz.stignarnia.uiShow.sections.ratings.cases.ShowDetailsRatingSpoilersCase
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ShowDetailsRatingsViewModel
  @Inject
  constructor(
    private val ratingsCase: ShowDetailsRatingCase,
    private val ratingsSpoilersCase: ShowDetailsRatingSpoilersCase,
  ) : ViewModel() {
    private var loadedImdbId: String? = null

    private val showState = MutableStateFlow<Show?>(null)
    private val ratingsState = MutableStateFlow<Ratings?>(null)
    private val isRefreshingRatingsState = MutableStateFlow(false)

    fun loadRatings(show: Show) {
      val imdbId = show.ids.imdb.id
      if (loadedImdbId != null) return
      loadedImdbId = imdbId

      viewModelScope.launch {
        showState.value = show

        val externalRatings =
          Ratings(
            tmdb = Ratings.Value(String.format(Locale.ENGLISH, "%.1f", show.rating), false),
            imdb = Ratings.Value(null, true),
            metascore = Ratings.Value(null, true),
            rottenTomatoes = Ratings.Value(null, true),
          )

        isRefreshingRatingsState.value = false
        ratingsState.value = ratingsSpoilersCase.hideSpoilerRatings(show, externalRatings)

        // Every external rating comes from OMDb, so with no key there is nothing to fetch.
        // Flag it rather than leaving three slots blank for no reason.
        if (!ratingsCase.hasOmdbApiKey()) {
          val settled = externalRatings.settled().copy(isOmdbKeyMissing = true)
          ratingsState.value = ratingsSpoilersCase.hideSpoilerRatings(show, settled)
          return@launch
        }

        // External ratings are looked up by IMDb id.
        // Without one there is nothing to ask OMDb for, so settle rather than spinning on a request that could only fail.
        if (imdbId.isBlank()) {
          ratingsState.value = ratingsSpoilersCase.hideSpoilerRatings(show, externalRatings.settled())
          return@launch
        }

        try {
          val ratings = ratingsCase.loadExternalRatings(show)
          ratingsState.value = ratingsSpoilersCase.hideSpoilerRatings(show, ratings)
        } catch (error: Throwable) {
          ratingsState.value = ratingsSpoilersCase.hideSpoilerRatings(show, externalRatings.settled())
          rethrowCancellation(error)
        }
      }
    }

    /**
     * Clears the loading flags so a failed lookup renders as absent rather than spinning forever.
     */
    private fun Ratings.settled() =
      copy(
        imdb = Ratings.Value(imdb?.value, false),
        metascore = Ratings.Value(metascore?.value, false),
        rottenTomatoes = Ratings.Value(rottenTomatoes?.value, false),
      )

    fun refreshRatings() {
      val show = showState.value
      val ratings = ratingsState.value
      viewModelScope.launch {
        if (show != null && ratings != null) {
          isRefreshingRatingsState.value = true
          ratingsState.value = ratingsSpoilersCase.hideSpoilerRatings(show, ratings)
        }
      }
    }

    val uiState =
      combine(
        showState,
        ratingsState,
        isRefreshingRatingsState,
      ) { s1, s2, s3 ->
        ShowDetailsRatingsUiState(
          show = s1,
          ratings = s2,
          isRefreshingRatings = s3,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
        initialValue = ShowDetailsRatingsUiState(),
      )
  }
