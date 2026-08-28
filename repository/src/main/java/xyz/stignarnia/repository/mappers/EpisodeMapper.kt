package xyz.stignarnia.repository.mappers

import xyz.stignarnia.common.extensions.toZonedDateTime
import xyz.stignarnia.uiModel.Episode
import xyz.stignarnia.uiModel.IdImdb
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.IdTvdb
import xyz.stignarnia.uiModel.Ids
import xyz.stignarnia.uiModel.Season
import java.time.ZonedDateTime
import javax.inject.Inject
import xyz.stignarnia.dataLocal.database.model.Episode as EpisodeDb
import xyz.stignarnia.dataRemote.catalog.model.Episode as EpisodeNetwork

class EpisodeMapper
  @Inject
  constructor(
    private val idsMapper: IdsMapper,
  ) {
    fun fromNetwork(episode: EpisodeNetwork) =
      Episode(
        season = episode.season ?: -1,
        number = episode.number ?: -1,
        title = episode.title ?: "",
        ids = idsMapper.fromNetwork(episode.ids),
        overview = episode.overview ?: "",
        rating = episode.rating ?: 0F,
        votes = episode.votes ?: 0,
        commentCount = episode.comment_count ?: 0,
        firstAired = episode.first_aired.toZonedDateTime(),
        runtime = episode.runtime ?: -1,
        numberAbs = episode.number_abs,
        lastWatchedAt = episode.last_watched_at.toZonedDateTime(),
      )

    fun toNetwork(episode: Episode) =
      EpisodeNetwork(
        ids = idsMapper.toNetwork(episode.ids),
        season = episode.season,
        number = episode.number,
        number_abs = episode.numberAbs,
        title = episode.title,
        overview = episode.overview,
        rating = episode.rating,
        votes = episode.votes,
        comment_count = episode.commentCount,
        first_aired = episode.firstAired.toString(),
        runtime = episode.runtime,
        last_watched_at = episode.lastWatchedAt.toString(),
      )

    fun toDatabase(
      episode: Episode,
      season: Season,
      showId: IdTmdb,
      isWatched: Boolean,
      lastExportedAt: ZonedDateTime?,
      lastWatchedAt: ZonedDateTime?,
    ): EpisodeDb =
      EpisodeDb(
        idTmdb = episode.ids.tmdb.id,
        idSeason = season.ids.tmdb.id,
        idShowTmdb = showId.id,
        idShowTvdb = episode.ids.tvdb.id,
        idShowImdb = episode.ids.imdb.id,
        seasonNumber = season.number,
        episodeNumber = episode.number,
        episodeNumberAbs = episode.numberAbs,
        episodeOverview = episode.overview,
        title = episode.title,
        firstAired = episode.firstAired,
        commentsCount = episode.commentCount,
        rating = episode.rating,
        runtime = episode.runtime,
        votesCount = episode.votes,
        isWatched = isWatched,
        lastExportedAt = lastExportedAt,
        lastWatchedAt = lastWatchedAt,
      )

    fun fromDatabase(episodeDb: EpisodeDb) =
      Episode(
        ids =
          Ids.EMPTY.copy(
            tvdb = IdTvdb(episodeDb.idShowTvdb),
            imdb = IdImdb(episodeDb.idShowImdb),
            tmdb = IdTmdb(episodeDb.idShowTmdb),
          ),
        title = episodeDb.title,
        number = episodeDb.episodeNumber,
        numberAbs = episodeDb.episodeNumberAbs,
        season = episodeDb.seasonNumber,
        overview = episodeDb.episodeOverview,
        commentCount = episodeDb.commentsCount,
        firstAired = episodeDb.firstAired,
        rating = episodeDb.rating,
        runtime = episodeDb.runtime,
        votes = episodeDb.votesCount,
        lastWatchedAt = episodeDb.lastWatchedAt,
      )
  }
