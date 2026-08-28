package xyz.stignarnia.uiBase.common.sheets.ratings.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.errors.ErrorHelper
import xyz.stignarnia.common.errors.ShelflyError
import xyz.stignarnia.repository.RatingsRepository
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Ids
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.UserRating
import javax.inject.Inject

@ViewModelScoped
class RatingsShowCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val ratingsRepository: RatingsRepository,
  ) {
    companion object {
      private val RATING_VALID_RANGE = 1..10
    }

    suspend fun loadRating(idTmdb: IdTmdb): UserRating =
      withContext(dispatchers.IO) {
        val show = Show.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = idTmdb))
        try {
          val rating = ratingsRepository.shows.loadRatings(listOf(show))
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
        val show = Show.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = idTmdb))
        ratingsRepository.shows.addRating(
          show = show,
          rating = rating,
        )
      } catch (error: Throwable) {
        handleError(error)
      }
    }

    suspend fun deleteRating(idTmdb: IdTmdb) =
      withContext(dispatchers.IO) {
        val show = Show.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = idTmdb))
        try {
          ratingsRepository.shows.deleteRating(
            show = show,
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
