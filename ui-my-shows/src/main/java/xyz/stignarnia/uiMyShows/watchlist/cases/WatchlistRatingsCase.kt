package xyz.stignarnia.uiMyShows.watchlist.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.RatingsRepository
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.UserRating
import javax.inject.Inject

@ViewModelScoped
class WatchlistRatingsCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val ratingsRepository: RatingsRepository,
  ) {
    suspend fun loadRatings(): Map<IdTmdb, UserRating?> =
      withContext(dispatchers.IO) {
        ratingsRepository.shows.loadShowsRatings().associateBy { it.idTmdb }
      }
  }
