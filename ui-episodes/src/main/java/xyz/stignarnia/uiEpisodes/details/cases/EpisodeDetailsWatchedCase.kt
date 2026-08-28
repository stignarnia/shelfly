package xyz.stignarnia.uiEpisodes.details.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.dataLocal.sources.EpisodesLocalDataSource
import xyz.stignarnia.uiModel.Episode
import xyz.stignarnia.uiModel.IdTmdb
import java.time.ZonedDateTime
import javax.inject.Inject

@ViewModelScoped
class EpisodeDetailsWatchedCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val episodesDataSource: EpisodesLocalDataSource,
  ) {
    suspend fun getLastWatchedAt(
      showId: IdTmdb,
      episode: Episode,
    ): ZonedDateTime? =
      withContext(dispatchers.IO) {
        episodesDataSource.getById(showId.id, episode.ids.tmdb.id)?.lastWatchedAt
      }
  }
