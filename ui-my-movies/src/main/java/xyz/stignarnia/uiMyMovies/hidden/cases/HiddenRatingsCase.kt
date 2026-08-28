package xyz.stignarnia.uiMyMovies.hidden.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.RatingsRepository
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.UserRating
import javax.inject.Inject

@ViewModelScoped
class HiddenRatingsCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val ratingsRepository: RatingsRepository,
  ) {
    suspend fun loadRatings(): Map<IdTmdb, UserRating?> =
      withContext(dispatchers.IO) {
        ratingsRepository.movies.loadMoviesRatings().associateBy { it.idTmdb }
      }
  }
