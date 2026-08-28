package xyz.stignarnia.uiEpisodes.details.cases

import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.dataLocal.sources.EpisodesLocalDataSource
import xyz.stignarnia.dataLocal.sources.MyShowsLocalDataSource
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.uiModel.Episode
import xyz.stignarnia.uiModel.IdTmdb
import javax.inject.Inject

@ViewModelScoped
class EpisodeDetailsSeasonCase
  @Inject
  constructor(
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

      val episodes =
        episodesDataSource
          .getAllByShowId(showId.id, episode.season)
          .map { mappers.episode.fromDatabase(it) }
          .sortedBy { it.number }

      if (episodes.isNotEmpty()) return episodes
      return seasonEpisodes?.map {
        Episode.EMPTY.copy(season = episode.season, number = it)
      } ?: emptyList()
    }
  }
