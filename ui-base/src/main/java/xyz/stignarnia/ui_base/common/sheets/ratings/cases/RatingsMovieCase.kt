package xyz.stignarnia.ui_base.common.sheets.ratings.cases

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.errors.ErrorHelper
import xyz.stignarnia.common.errors.ShelflyError
import xyz.stignarnia.repository.RatingsRepository
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Ids
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.UserRating
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class RatingsMovieCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val ratingsRepository: RatingsRepository,
) {

  companion object {
    private val RATING_VALID_RANGE = 1..10
  }

  suspend fun loadRating(idTmdb: IdTmdb): UserRating =
    withContext(dispatchers.IO) {
      val movie = Movie.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = idTmdb))
      try {
        val rating = ratingsRepository.movies.loadRatings(listOf(movie))
        rating.firstOrNull() ?: UserRating.EMPTY
      } catch (error: Throwable) {
        handleError(error)
        UserRating.EMPTY
      }
    }

  suspend fun saveRating(
    idTmdb: IdTmdb,
    rating: Int,
  ) = withContext(dispatchers.IO) {
    check(rating in RATING_VALID_RANGE)

    try {
      val movie = Movie.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = idTmdb))
      ratingsRepository.movies.addRating(
        movie = movie,
        rating = rating,
      )
    } catch (error: Throwable) {
      handleError(error)
    }
  }

  suspend fun deleteRating(idTmdb: IdTmdb) =
    withContext(dispatchers.IO) {
      val movie = Movie.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = idTmdb))
      try {
        ratingsRepository.movies.deleteRating(
          movie = movie,
        )
      } catch (error: Throwable) {
        handleError(error)
      }
    }

  private suspend fun handleError(error: Throwable) {
    val parsedError = ErrorHelper.parse(error)
    if (parsedError is ShelflyError.UnauthorizedError) {
    }
    throw error
  }
}
