package xyz.stignarnia.ui_show.episodes.cases

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.RatingsRepository
import xyz.stignarnia.ui_model.Episode
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Season
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class EpisodesRatingCase @Inject constructor(
  private val dispatchers: CoroutineDispatchers,
  private val ratingsRepository: RatingsRepository,
) {

  suspend fun loadRating(
    showId: IdTmdb,
    episode: Episode,
  ) = withContext(dispatchers.IO) {
    ratingsRepository.shows.loadRating(showId, episode)
  }

  suspend fun loadRating(
    showId: IdTmdb,
    season: Season,
  ) = withContext(dispatchers.IO) {
    ratingsRepository.shows.loadRating(showId, season)
  }
}
