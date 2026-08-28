package xyz.stignarnia.uiMovie.sections.ratings.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.settings.SettingsSpoilersRepository
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.Ratings
import xyz.stignarnia.uiMovie.MovieDetailsUiState.FollowedState
import xyz.stignarnia.uiMovie.cases.MovieDetailsHiddenCase
import xyz.stignarnia.uiMovie.cases.MovieDetailsMyMoviesCase
import xyz.stignarnia.uiMovie.cases.MovieDetailsWatchlistCase
import javax.inject.Inject

@ViewModelScoped
class MovieDetailsRatingSpoilersCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val myMoviesCase: MovieDetailsMyMoviesCase,
    private val watchlistCase: MovieDetailsWatchlistCase,
    private val hiddenCase: MovieDetailsHiddenCase,
    private val settingsSpoilersRepository: SettingsSpoilersRepository,
  ) {
    suspend fun hideSpoilerRatings(
      movie: Movie,
      ratings: Ratings,
    ): Ratings =
      withContext(dispatchers.IO) {
        val spoilers = settingsSpoilersRepository.getAll()

        val isMy = async { myMoviesCase.getMyMovie(movie) }
        val isWatchlist = async { watchlistCase.isWatchlist(movie) }
        val isHidden = async { hiddenCase.isHidden(movie) }

        val state =
          FollowedState(
            isMyMovie = isMy.await() != null,
            isWatchlist = isWatchlist.await(),
            isHidden = isHidden.await(),
            withAnimation = false,
          )

        val isMyHidden = spoilers.isMyMoviesRatingsHidden && state.isMyMovie
        val isWatchlistHidden = spoilers.isWatchlistMoviesRatingsHidden && state.isWatchlist
        val isHiddenHidden = spoilers.isHiddenMoviesRatingsHidden && state.isHidden
        val isNotCollectedHidden = spoilers.isNotCollectedMoviesRatingsHidden && !state.isInCollection()

        return@withContext ratings.copy(
          isHidden = isMyHidden || isWatchlistHidden || isHiddenHidden || isNotCollectedHidden,
        )
      }
  }
