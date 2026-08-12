package com.michaldrabik.ui_my_movies.mymovies.cases

import com.michaldrabik.common.dispatchers.CoroutineDispatchers
import com.michaldrabik.repository.RatingsRepository
import com.michaldrabik.ui_model.IdTmdb
import com.michaldrabik.ui_model.UserRating
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class MyMoviesRatingsCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val ratingsRepository: RatingsRepository,
) {

  suspend fun loadRatings(): Map<IdTmdb, UserRating?> =
    withContext(dispatchers.IO) {
      ratingsRepository.movies.loadMoviesRatings().associateBy { it.idTmdb }
    }
}
