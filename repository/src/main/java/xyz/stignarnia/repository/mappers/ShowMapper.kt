package xyz.stignarnia.repository.mappers

import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.uiModel.AirTime
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.ShowStatus
import javax.inject.Inject
import xyz.stignarnia.dataLocal.database.model.Show as ShowDb
import xyz.stignarnia.dataRemote.catalog.model.AirTime as AirTimeNetwork
import xyz.stignarnia.dataRemote.catalog.model.Show as ShowNetwork

class ShowMapper
  @Inject
  constructor(
    private val idsMapper: IdsMapper,
  ) {
    fun fromNetwork(show: ShowNetwork) =
      Show(
        idsMapper.fromNetwork(show.ids),
        show.title ?: "",
        show.year ?: -1,
        show.overview ?: "",
        show.first_aired ?: "",
        show.runtime ?: -1,
        AirTime(
          show.airs?.day ?: "",
          show.airs?.time ?: "",
          show.airs?.timezone ?: "",
        ),
        show.certification ?: "",
        show.network ?: "",
        show.network_logo_path ?: "",
        show.country ?: "",
        show.trailer ?: "",
        show.homepage ?: "",
        ShowStatus.fromKey(show.status),
        show.rating ?: -1F,
        show.votes ?: -1,
        show.comment_count ?: -1,
        show.genres ?: emptyList(),
        show.aired_episodes ?: -1,
        nowUtcMillis(),
        nowUtcMillis(),
      )

    fun toNetwork(show: Show) =
      ShowNetwork(
        idsMapper.toNetwork(show.ids),
        show.title,
        show.year,
        show.overview,
        show.firstAired,
        show.runtime,
        AirTimeNetwork(
          show.airTime.day,
          show.airTime.time,
          show.airTime.timezone,
        ),
        show.certification,
        show.network,
        show.networkLogoPath,
        show.country,
        show.trailer,
        show.homepage,
        show.status.key,
        show.rating,
        show.votes,
        show.commentCount,
        show.genres,
        show.airedEpisodes,
      )

    fun fromDatabase(show: ShowDb) =
      Show(
        idsMapper.fromDatabase(show),
        show.title,
        show.year,
        show.overview,
        show.firstAired,
        show.runtime,
        AirTime(show.airtimeDay, show.airtimeTime, show.airtimeTimezone),
        show.certification,
        show.network,
        show.networkLogoPath,
        show.country,
        show.trailer,
        show.homepage,
        ShowStatus.fromKey(show.status),
        show.rating,
        show.votes,
        show.commentCount,
        show.genres.split(","),
        show.airedEpisodes,
        show.createdAt,
        show.updatedAt,
      )

    fun toDatabase(show: Show) =
      ShowDb(
        show.tmdbId,
        show.ids.tvdb.id,
        show.ids.imdb.id,
        show.ids.slug.id,
        show.ids.tvrage.id,
        show.title,
        show.year,
        show.overview,
        show.firstAired,
        show.runtime,
        show.airTime.day,
        show.airTime.time,
        show.airTime.timezone,
        show.certification,
        show.network,
        show.networkLogoPath,
        show.country,
        show.trailer,
        show.homepage,
        show.status.key,
        show.rating,
        show.votes,
        show.commentCount,
        show.genres.joinToString(","),
        show.airedEpisodes,
        show.createdAt,
        nowUtcMillis(),
      )
  }
