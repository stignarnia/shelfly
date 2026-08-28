package xyz.stignarnia.uiShow.sections.nextepisode.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.dataLocal.sources.EpisodesLocalDataSource
import xyz.stignarnia.uiModel.Episode
import xyz.stignarnia.uiModel.Show
import javax.inject.Inject

@ViewModelScoped
class ShowDetailsWatchedCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val episodesLocalDataSource: EpisodesLocalDataSource,
  ) {
    suspend fun isWatched(
      show: Show,
      episode: Episode,
    ): Boolean =
      withContext(dispatchers.IO) {
        return@withContext episodesLocalDataSource.isEpisodeWatched(
          showTmdbId = show.tmdbId,
          episodeTmdbId = episode.ids.tmdb.id,
        )
      }
  }
