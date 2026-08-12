package xyz.stignarnia.ui_episodes.details.cases

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.data_local.sources.EpisodesLocalDataSource
import xyz.stignarnia.ui_model.Episode
import xyz.stignarnia.ui_model.IdTmdb
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import javax.inject.Inject

@ViewModelScoped
class EpisodeDetailsWatchedCase @Inject constructor(
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
