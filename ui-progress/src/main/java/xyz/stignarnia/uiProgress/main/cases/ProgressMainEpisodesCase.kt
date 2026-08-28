package xyz.stignarnia.uiProgress.main.cases

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.dataLocal.sources.EpisodesLocalDataSource
import xyz.stignarnia.repository.EpisodesManager
import xyz.stignarnia.repository.settings.SettingsSpoilersRepository
import xyz.stignarnia.uiModel.Episode
import xyz.stignarnia.uiModel.EpisodeBundle
import xyz.stignarnia.uiModel.Show
import java.time.ZonedDateTime
import javax.inject.Inject

@ViewModelScoped
class ProgressMainEpisodesCase
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val episodesManager: EpisodesManager,
    private val spoilersSettings: SettingsSpoilersRepository,
    private val localDataSource: EpisodesLocalDataSource,
  ) {
    suspend fun setEpisodeWatched(
      bundle: EpisodeBundle,
      customDate: ZonedDateTime?,
    ) {
      episodesManager.setEpisodeWatched(bundle, customDate)
    }

    suspend fun isWatched(
      show: Show,
      episode: Episode,
    ): Boolean {
      return withContext(dispatchers.IO) {
        // No need to query DB if spoilers settings are all off in that case.
        if (!(
            spoilersSettings.isEpisodesTitleHidden ||
              spoilersSettings.isEpisodesDescriptionHidden ||
              spoilersSettings.isEpisodesImageHidden ||
              spoilersSettings.isEpisodesRatingHidden
          )
        ) {
          return@withContext false
        }
        return@withContext localDataSource.isEpisodeWatched(show.tmdbId, episode.ids.tmdb.id)
      }
    }
  }
