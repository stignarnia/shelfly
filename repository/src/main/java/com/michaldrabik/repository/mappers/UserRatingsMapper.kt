package com.michaldrabik.repository.mappers

import com.michaldrabik.common.extensions.nowUtc
import com.michaldrabik.data_local.database.model.Rating
import com.michaldrabik.ui_model.Episode
import com.michaldrabik.ui_model.IdTmdb
import com.michaldrabik.ui_model.Movie
import com.michaldrabik.ui_model.Season
import com.michaldrabik.ui_model.Show
import com.michaldrabik.ui_model.UserRating
import java.time.ZonedDateTime
import javax.inject.Inject

class UserRatingsMapper @Inject constructor() {

  fun fromDatabase(entity: Rating) =
    UserRating(
      idTmdb = IdTmdb(entity.idTmdb),
      rating = entity.rating,
      ratedAt = entity.ratedAt,
    )

  fun toDatabaseMovie(
    movie: Movie,
    rating: Int,
    ratedAt: ZonedDateTime,
  ) = Rating(
    idTmdb = movie.tmdbId,
    type = "movie",
    rating = rating,
    seasonNumber = null,
    episodeNumber = null,
    ratedAt = ratedAt,
    createdAt = nowUtc(),
    updatedAt = nowUtc(),
  )

  fun toDatabaseShow(
    show: Show,
    rating: Int,
    ratedAt: ZonedDateTime,
  ) = Rating(
    idTmdb = show.tmdbId,
    type = "show",
    rating = rating,
    seasonNumber = null,
    episodeNumber = null,
    ratedAt = ratedAt,
    createdAt = nowUtc(),
    updatedAt = nowUtc(),
  )

  fun toDatabaseEpisode(
    episode: Episode,
    rating: Int,
    ratedAt: ZonedDateTime,
  ) = Rating(
    idTmdb = episode.ids.tmdb.id,
    type = "episode",
    rating = rating,
    seasonNumber = episode.season,
    episodeNumber = episode.number,
    ratedAt = ratedAt,
    createdAt = nowUtc(),
    updatedAt = nowUtc(),
  )

  fun toDatabaseSeason(
    season: Season,
    rating: Int,
    ratedAt: ZonedDateTime,
  ) = Rating(
    idTmdb = season.ids.tmdb.id,
    type = "season",
    rating = rating,
    seasonNumber = season.number,
    episodeNumber = null,
    ratedAt = ratedAt,
    createdAt = nowUtc(),
    updatedAt = nowUtc(),
  )
}
