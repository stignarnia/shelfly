package xyz.stignarnia.repository.mappers

import xyz.stignarnia.data_local.database.model.Episode
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Ids
import xyz.stignarnia.ui_model.Season
import java.time.ZonedDateTime
import javax.inject.Inject
import xyz.stignarnia.data_local.database.model.Season as SeasonDb
import xyz.stignarnia.data_remote.catalog.model.Season as SeasonNetwork

class SeasonMapper @Inject constructor(
  private val idsMapper: IdsMapper,
  private val episodeMapper: EpisodeMapper,
) {

  fun fromNetwork(season: SeasonNetwork) =
    Season(
      idsMapper.fromNetwork(season.ids),
      season.number ?: -1,
      season.episode_count ?: -1,
      season.aired_episodes ?: -1,
      season.title ?: "",
      if (season.first_aired.isNullOrBlank()) null else ZonedDateTime.parse(season.first_aired),
      season.overview ?: "",
      season.rating ?: -1F,
      season.episodes?.map { episodeMapper.fromNetwork(it) } ?: emptyList(),
    )

  fun toNetwork(season: Season) =
    SeasonNetwork(
      ids = idsMapper.toNetwork(season.ids),
      number = season.number,
      episode_count = season.episodeCount,
      aired_episodes = season.airedEpisodes,
      title = season.title,
      first_aired = season.firstAired.toString(),
      overview = season.overview,
      rating = season.rating,
      episodes = season.episodes.map { episodeMapper.toNetwork(it) },
    )

  fun fromDatabase(
    seasonDb: SeasonDb,
    episodes: List<Episode> = emptyList(),
  ) = Season(
    Ids.EMPTY.copy(tmdb = IdTmdb(seasonDb.idTmdb)),
    seasonDb.seasonNumber,
    seasonDb.episodesCount,
    seasonDb.episodesAiredCount,
    seasonDb.seasonTitle,
    seasonDb.seasonFirstAired,
    seasonDb.seasonOverview,
    seasonDb.rating ?: -1F,
    episodes.map { episodeMapper.fromDatabase(it) },
  )

  fun toDatabase(
    season: Season,
    showId: IdTmdb,
    isWatched: Boolean,
  ): SeasonDb =
    SeasonDb(
      season.ids.tmdb.id,
      showId.id,
      season.number,
      season.title,
      season.overview,
      season.firstAired,
      season.episodeCount,
      season.airedEpisodes,
      season.rating,
      isWatched,
    )
}
