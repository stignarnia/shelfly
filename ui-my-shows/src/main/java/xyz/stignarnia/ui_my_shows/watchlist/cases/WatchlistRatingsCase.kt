package xyz.stignarnia.ui_my_shows.watchlist.cases

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.RatingsRepository
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.UserRating
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class WatchlistRatingsCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val ratingsRepository: RatingsRepository,
) {

  suspend fun loadRatings(): Map<IdTmdb, UserRating?> =
    withContext(dispatchers.IO) {
      ratingsRepository.shows.loadShowsRatings().associateBy { it.idTmdb }
    }
}
