package xyz.stignarnia.ui_base.common.sheets.ratings.cases

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.common.errors.ErrorHelper
import xyz.stignarnia.common.errors.ShelflyError
import xyz.stignarnia.repository.RatingsRepository
import xyz.stignarnia.ui_model.Episode
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.UserRating
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class RatingsEpisodeCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val ratingsRepository: RatingsRepository,
) {

  companion object {
    private val RATING_VALID_RANGE = 1..10
  }

  suspend fun loadRating(
    showId: IdTmdb,
    seasonNumber: Int,
    episodeNumber: Int,
  ): UserRating =
    withContext(dispatchers.IO) {
      try {
        ratingsRepository.shows.loadRating(showId, episode(seasonNumber, episodeNumber)) ?: UserRating.EMPTY
      } catch (error: Throwable) {
        handleError(error)
        UserRating.EMPTY
      }
    }

  suspend fun saveRating(
    showId: IdTmdb,
    seasonNumber: Int,
    episodeNumber: Int,
    rating: Int,
  ) = withContext(dispatchers.IO) {
    check(rating in RATING_VALID_RANGE)

    try {
      ratingsRepository.shows.addRating(
        showId = showId,
        episode = episode(seasonNumber, episodeNumber),
        rating = rating,
      )
    } catch (error: Throwable) {
      handleError(error)
    }
  }

  suspend fun deleteRating(
    showId: IdTmdb,
    seasonNumber: Int,
    episodeNumber: Int,
  ) = withContext(dispatchers.IO) {
    try {
      ratingsRepository.shows.deleteRating(
        showId = showId,
        episode = episode(seasonNumber, episodeNumber),
      )
    } catch (error: Throwable) {
      handleError(error)
    }
  }

  // The rating is keyed by show, season and episode number, so nothing else is
  // read off the episode here.
  private fun episode(
    seasonNumber: Int,
    episodeNumber: Int,
  ) = Episode.EMPTY.copy(season = seasonNumber, number = episodeNumber)

  private suspend fun handleError(error: Throwable) {
    val parsedError = ErrorHelper.parse(error)
    if (parsedError is ShelflyError.UnauthorizedError) {
    }
    throw error
  }
}
