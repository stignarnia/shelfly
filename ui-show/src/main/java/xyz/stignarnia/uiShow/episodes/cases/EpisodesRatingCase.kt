package xyz.stignarnia.uiShow.episodes.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.repository.RatingsRepository
import xyz.stignarnia.uiModel.Episode
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Season
import javax.inject.Inject

@ViewModelScoped
class EpisodesRatingCase
  @Inject
  constructor(
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
