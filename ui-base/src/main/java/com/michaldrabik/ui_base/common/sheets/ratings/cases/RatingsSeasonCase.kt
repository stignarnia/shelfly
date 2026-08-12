package com.michaldrabik.ui_base.common.sheets.ratings.cases

import com.michaldrabik.common.dispatchers.CoroutineDispatchers
import com.michaldrabik.common.errors.ErrorHelper
import com.michaldrabik.common.errors.ShowlyError
import com.michaldrabik.repository.RatingsRepository
import com.michaldrabik.ui_model.IdTmdb
import com.michaldrabik.ui_model.Ids
import com.michaldrabik.ui_model.Season
import com.michaldrabik.ui_model.UserRating
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class RatingsSeasonCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val ratingsRepository: RatingsRepository,
) {

  companion object {
    private val RATING_VALID_RANGE = 1..10
  }

  suspend fun loadRating(idTmdb: IdTmdb): UserRating =
    withContext(dispatchers.IO) {
      val season = Season.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = idTmdb))
      try {
        val rating = ratingsRepository.shows.loadRatingsSeasons(listOf(season))
        rating.firstOrNull() ?: UserRating.EMPTY
      } catch (error: Throwable) {
        handleError(error)
        UserRating.EMPTY
      }
    }

  suspend fun saveRating(
    idTmdb: IdTmdb,
    rating: Int,
    seasonNumber: Int,
  ) = withContext(dispatchers.IO) {
    check(rating in RATING_VALID_RANGE)

    val season = Season.EMPTY.copy(
      ids = Ids.EMPTY.copy(tmdb = idTmdb),
      number = seasonNumber,
    )

    try {
      ratingsRepository.shows.addRating(
        season = season,
        rating = rating,
      )
    } catch (error: Throwable) {
      handleError(error)
    }
  }

  suspend fun deleteRating(idTmdb: IdTmdb) =
    withContext(dispatchers.IO) {
      val season = Season.EMPTY.copy(ids = Ids.EMPTY.copy(tmdb = idTmdb))
      try {
        ratingsRepository.shows.deleteRating(
          season = season,
        )
      } catch (error: Throwable) {
        handleError(error)
      }
    }

  private suspend fun handleError(error: Throwable) {
    val showlyError = ErrorHelper.parse(error)
    if (showlyError is ShowlyError.UnauthorizedError) {
    }
    throw error
  }
}
