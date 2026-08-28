package xyz.stignarnia.uiShow.sections.ratings.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.settings.SettingsSpoilersRepository
import xyz.stignarnia.uiModel.Ratings
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiShow.ShowDetailsUiState.FollowedState
import xyz.stignarnia.uiShow.cases.ShowDetailsHiddenCase
import xyz.stignarnia.uiShow.cases.ShowDetailsMyShowsCase
import xyz.stignarnia.uiShow.cases.ShowDetailsWatchlistCase
import javax.inject.Inject

@ViewModelScoped
class ShowDetailsRatingSpoilersCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val watchlistCase: ShowDetailsWatchlistCase,
    private val hiddenCase: ShowDetailsHiddenCase,
    private val myShowsCase: ShowDetailsMyShowsCase,
    private val settingsSpoilersRepository: SettingsSpoilersRepository,
  ) {
    suspend fun hideSpoilerRatings(
      show: Show,
      ratings: Ratings,
    ): Ratings =
      withContext(dispatchers.IO) {
        val spoilers = settingsSpoilersRepository.getAll()

        val isMy = async { myShowsCase.isMyShows(show) }
        val isWatchlist = async { watchlistCase.isWatchlist(show) }
        val isHidden = async { hiddenCase.isHidden(show) }

        val state =
          FollowedState(
            isMyShows = isMy.await(),
            isWatchlist = isWatchlist.await(),
            isHidden = isHidden.await(),
            withAnimation = false,
          )

        val isMyHidden = spoilers.isMyShowsRatingsHidden && state.isMyShows
        val isWatchlistHidden = spoilers.isWatchlistShowsRatingsHidden && state.isWatchlist
        val isHiddenHidden = spoilers.isHiddenShowsRatingsHidden && state.isHidden
        val isNotCollectedHidden = spoilers.isNotCollectedShowsRatingsHidden && !state.isInCollection()

        return@withContext ratings.copy(
          isHidden = isMyHidden || isWatchlistHidden || isHiddenHidden || isNotCollectedHidden,
          isTapToReveal = settingsSpoilersRepository.isTapToReveal,
        )
      }
  }
