package xyz.stignarnia.ui_progress.main.cases

import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.data_local.sources.EpisodesLocalDataSource
import xyz.stignarnia.repository.EpisodesManager
import xyz.stignarnia.repository.settings.SettingsSpoilersRepository
import xyz.stignarnia.ui_model.Episode
import xyz.stignarnia.ui_model.EpisodeBundle
import xyz.stignarnia.ui_model.Show
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import javax.inject.Inject

@ViewModelScoped
class ProgressMainEpisodesCase @Inject constructor(
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
