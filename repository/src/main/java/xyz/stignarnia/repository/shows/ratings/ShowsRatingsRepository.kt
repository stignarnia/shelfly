package xyz.stignarnia.repository.shows.ratings

import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.dataLocal.database.model.Rating
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.uiModel.Episode
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Season
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.UserRating
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Season and episode ratings hang off the show that owns them, so every call about one takes the show's TMDB id - see [Rating].
 */
@Singleton
class ShowsRatingsRepository
  @Inject
  constructor(
    val external: ShowsExternalRatingsRepository,
    private val localSource: LocalDataSource,
    private val mappers: Mappers,
  ) {
    companion object {
      private const val CHUNK_SIZE = 250
    }

    suspend fun loadShowsRatings(): List<UserRating> {
      val ratings = localSource.ratings.getAllByType(Rating.TYPE_SHOW)
      return ratings.map {
        mappers.userRatings.fromDatabase(it)
      }
    }

    suspend fun loadSeasonsRatings(): List<Rating> = localSource.ratings.getAllByType(Rating.TYPE_SEASON)

    suspend fun loadEpisodesRatings(): List<Rating> = localSource.ratings.getAllByType(Rating.TYPE_EPISODE)

    suspend fun loadRatings(shows: List<Show>): List<UserRating> {
      val ratings = mutableListOf<Rating>()
      shows.chunked(CHUNK_SIZE).forEach { chunk ->
        val items = localSource.ratings.getAllByType(chunk.map { it.tmdbId }, Rating.TYPE_SHOW)
        ratings.addAll(items)
      }
      return ratings.map {
        mappers.userRatings.fromDatabase(it)
      }
    }

    /** Every season rating for [showId], keyed by season number. */
    suspend fun loadSeasonRatings(showId: IdTmdb): Map<Int, UserRating> =
      localSource.ratings
        .getSeasonRatings(showId.id)
        .associate { it.seasonNumber to mappers.userRatings.fromDatabase(it) }

    suspend fun loadRating(
      showId: IdTmdb,
      season: Season,
    ): UserRating? =
      localSource.ratings
        .getSeasonRating(showId.id, season.number)
        ?.let { mappers.userRatings.fromDatabase(it) }

    suspend fun loadRating(
      showId: IdTmdb,
      episode: Episode,
    ): UserRating? =
      localSource.ratings
        .getEpisodeRating(showId.id, episode.season, episode.number)
        ?.let { mappers.userRatings.fromDatabase(it) }

    suspend fun addRating(
      show: Show,
      rating: Int,
    ) {
      val entity = mappers.userRatings.toDatabaseShow(show, rating, nowUtc())
      localSource.ratings.replace(entity)
    }

    suspend fun addRating(
      showId: IdTmdb,
      episode: Episode,
      rating: Int,
    ) {
      val entity = mappers.userRatings.toDatabaseEpisode(showId, episode, rating, nowUtc())
      localSource.ratings.replace(entity)
    }

    suspend fun addRating(
      showId: IdTmdb,
      season: Season,
      rating: Int,
    ) {
      val entity = mappers.userRatings.toDatabaseSeason(showId, season, rating, nowUtc())
      localSource.ratings.replace(entity)
    }

    suspend fun deleteRating(show: Show) {
      localSource.ratings.deleteByKey(
        tmdbId = show.tmdbId,
        type = Rating.TYPE_SHOW,
        seasonNumber = Rating.NO_NUMBER,
        episodeNumber = Rating.NO_NUMBER,
      )
    }

    suspend fun deleteRating(
      showId: IdTmdb,
      season: Season,
    ) {
      localSource.ratings.deleteByKey(
        tmdbId = showId.id,
        type = Rating.TYPE_SEASON,
        seasonNumber = season.number,
        episodeNumber = Rating.NO_NUMBER,
      )
    }

    suspend fun deleteRating(
      showId: IdTmdb,
      episode: Episode,
    ) {
      localSource.ratings.deleteByKey(
        tmdbId = showId.id,
        type = Rating.TYPE_EPISODE,
        seasonNumber = episode.season,
        episodeNumber = episode.number,
      )
    }
  }
