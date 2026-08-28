package xyz.stignarnia.uiBase.common.sheets.ratings.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.errors.ErrorHelper
import xyz.stignarnia.common.errors.ShelflyError
import xyz.stignarnia.repository.RatingsRepository
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Season
import xyz.stignarnia.uiModel.UserRating
import javax.inject.Inject

@ViewModelScoped
class RatingsSeasonCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val ratingsRepository: RatingsRepository,
  ) {
    companion object {
      private val RATING_VALID_RANGE = 1..10
    }

    suspend fun loadRating(
      showId: IdTmdb,
      seasonNumber: Int,
    ): UserRating =
      withContext(dispatchers.IO) {
        try {
          ratingsRepository.shows.loadRating(showId, season(seasonNumber)) ?: UserRating.EMPTY
        } catch (error: Throwable) {
          handleError(error)
          UserRating.EMPTY
        }
      }

    suspend fun saveRating(
      showId: IdTmdb,
      seasonNumber: Int,
      rating: Int,
    ) = withContext(dispatchers.IO) {
      check(rating in RATING_VALID_RANGE)

      try {
        ratingsRepository.shows.addRating(
          showId = showId,
          season = season(seasonNumber),
          rating = rating,
        )
      } catch (error: Throwable) {
        handleError(error)
      }
    }

    suspend fun deleteRating(
      showId: IdTmdb,
      seasonNumber: Int,
    ) = withContext(dispatchers.IO) {
      try {
        ratingsRepository.shows.deleteRating(
          showId = showId,
          season = season(seasonNumber),
        )
      } catch (error: Throwable) {
        handleError(error)
      }
    }

    // The rating is keyed by show and season number, so nothing else is read off the season here.
    private fun season(seasonNumber: Int) = Season.EMPTY.copy(number = seasonNumber)

    private suspend fun handleError(error: Throwable) {
      val parsedError = ErrorHelper.parse(error)
      if (parsedError is ShelflyError.UnauthorizedError) {
      }
      throw error
    }
  }
