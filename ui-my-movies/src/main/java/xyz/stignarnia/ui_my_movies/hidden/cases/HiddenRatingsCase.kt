package xyz.stignarnia.ui_my_movies.hidden.cases

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.RatingsRepository
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.UserRating
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class HiddenRatingsCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val ratingsRepository: RatingsRepository,
) {

  suspend fun loadRatings(): Map<IdTmdb, UserRating?> =
    withContext(dispatchers.IO) {
      ratingsRepository.movies.loadMoviesRatings().associateBy { it.idTmdb }
    }
}
