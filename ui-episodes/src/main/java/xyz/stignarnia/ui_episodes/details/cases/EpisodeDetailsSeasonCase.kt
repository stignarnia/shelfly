package xyz.stignarnia.ui_episodes.details.cases

import xyz.stignarnia.data_local.sources.EpisodesLocalDataSource
import xyz.stignarnia.data_local.sources.MyShowsLocalDataSource
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.ui_model.Episode
import xyz.stignarnia.ui_model.IdTmdb
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class EpisodeDetailsSeasonCase @Inject constructor(
  private val myShowsDataSource: MyShowsLocalDataSource,
  private val episodesDataSource: EpisodesLocalDataSource,
  private val mappers: Mappers,
) {

  suspend fun loadSeason(
    showId: IdTmdb,
    episode: Episode,
    seasonEpisodes: IntArray?,
  ): List<Episode> {
    val isMyShow = myShowsDataSource.checkExists(showId.id)
    if (!isMyShow) {
      return seasonEpisodes?.map {
        Episode.EMPTY.copy(season = episode.season, number = it)
      } ?: emptyList()
    }

    val episodes = episodesDataSource
      .getAllByShowId(showId.id, episode.season)
      .map { mappers.episode.fromDatabase(it) }
      .sortedBy { it.number }

    if (episodes.isNotEmpty()) return episodes
    return seasonEpisodes?.map {
      Episode.EMPTY.copy(season = episode.season, number = it)
    } ?: emptyList()
  }
}
