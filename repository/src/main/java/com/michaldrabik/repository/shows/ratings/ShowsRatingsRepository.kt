package com.michaldrabik.repository.shows.ratings

import com.michaldrabik.common.extensions.nowUtc
import com.michaldrabik.data_local.LocalDataSource
import com.michaldrabik.data_local.database.model.Rating
import com.michaldrabik.repository.mappers.Mappers
import com.michaldrabik.ui_model.Episode
import com.michaldrabik.ui_model.Season
import com.michaldrabik.ui_model.Show
import com.michaldrabik.ui_model.UserRating
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShowsRatingsRepository @Inject constructor(
  val external: ShowsExternalRatingsRepository,
  private val localSource: LocalDataSource,
  private val mappers: Mappers,
) {

  companion object {
    private const val TYPE_SHOW = "show"
    private const val TYPE_EPISODE = "episode"
    private const val TYPE_SEASON = "season"
    private const val CHUNK_SIZE = 250
  }

  suspend fun loadShowsRatings(): List<UserRating> {
    val ratings = localSource.ratings.getAllByType(TYPE_SHOW)
    return ratings.map {
      mappers.userRatings.fromDatabase(it)
    }
  }

  suspend fun loadSeasonsRatings(): List<Rating> {
    val ratings = localSource.ratings.getAllByType(TYPE_SEASON)
    return ratings
  }

  suspend fun loadEpisodesRatings(): List<Rating> {
    val ratings = localSource.ratings.getAllByType(TYPE_EPISODE)
    return ratings
  }

  suspend fun loadRatings(shows: List<Show>): List<UserRating> {
    val ratings = mutableListOf<Rating>()
    shows.chunked(CHUNK_SIZE).forEach { chunk ->
      val items = localSource.ratings.getAllByType(chunk.map { it.tmdbId }, TYPE_SHOW)
      ratings.addAll(items)
    }
    return ratings.map {
      mappers.userRatings.fromDatabase(it)
    }
  }

  suspend fun loadRatingsSeasons(seasons: List<Season>): List<UserRating> {
    val ratings = mutableListOf<Rating>()
    seasons.chunked(CHUNK_SIZE).forEach { chunk ->
      val items = localSource.ratings.getAllByType(chunk.map { it.ids.tmdb.id }, TYPE_SEASON)
      ratings.addAll(items)
    }
    return ratings.map {
      mappers.userRatings.fromDatabase(it)
    }
  }

  suspend fun loadRating(episode: Episode): UserRating? {
    val rating = localSource.ratings.getAllByType(listOf(episode.ids.tmdb.id), TYPE_EPISODE)
    return rating.firstOrNull()?.let {
      mappers.userRatings.fromDatabase(it)
    }
  }

  suspend fun loadRating(season: Season): UserRating? {
    val rating = localSource.ratings.getAllByType(listOf(season.ids.tmdb.id), TYPE_SEASON)
    return rating.firstOrNull()?.let {
      mappers.userRatings.fromDatabase(it)
    }
  }

  suspend fun addRating(
    show: Show,
    rating: Int,
  ) {
    val ratedAt = nowUtc()
    val entity = mappers.userRatings.toDatabaseShow(show, rating, ratedAt)
    localSource.ratings.replace(entity)
  }

  suspend fun addRating(
    episode: Episode,
    rating: Int,
  ) {
    val ratedAt = nowUtc()
    val entity = mappers.userRatings.toDatabaseEpisode(episode, rating, ratedAt)
    localSource.ratings.replace(entity)
  }

  suspend fun addRating(
    season: Season,
    rating: Int,
  ) {
    val ratedAt = nowUtc()
    val entity = mappers.userRatings.toDatabaseSeason(season, rating, ratedAt)
    localSource.ratings.replace(entity)
  }

  suspend fun deleteRating(show: Show) {
    localSource.ratings.deleteByType(show.tmdbId, TYPE_SHOW)
  }

  suspend fun deleteRating(episode: Episode) {
    localSource.ratings.deleteByType(episode.ids.tmdb.id, TYPE_EPISODE)
  }

  suspend fun deleteRating(season: Season) {
    localSource.ratings.deleteByType(season.ids.tmdb.id, TYPE_SEASON)
  }
}
