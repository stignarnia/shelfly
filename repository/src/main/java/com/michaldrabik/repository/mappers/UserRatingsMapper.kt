package com.michaldrabik.repository.mappers

import com.michaldrabik.common.extensions.nowUtc
import com.michaldrabik.data_local.database.model.Rating
import com.michaldrabik.data_remote.trakt.model.RatingResultEpisode
import com.michaldrabik.data_remote.trakt.model.RatingResultMovie
import com.michaldrabik.data_remote.trakt.model.RatingResultSeason
import com.michaldrabik.data_remote.trakt.model.RatingResultShow
import com.michaldrabik.ui_model.Episode
import com.michaldrabik.ui_model.IdTmdb
import com.michaldrabik.ui_model.Movie
import com.michaldrabik.ui_model.Season
import com.michaldrabik.ui_model.Show
import com.michaldrabik.ui_model.TraktRating
import java.time.ZonedDateTime
import javax.inject.Inject

class UserRatingsMapper @Inject constructor() {

  fun fromDatabase(entity: Rating) =
    TraktRating(
      idTmdb = IdTmdb(entity.idTmdb),
      rating = entity.rating,
      ratedAt = entity.ratedAt,
    )

  fun toDatabaseMovie(rating: RatingResultMovie) =
    Rating(
      idTmdb = rating.movie.ids.tmdb!!,
      type = "movie",
      rating = rating.rating,
      seasonNumber = null,
      episodeNumber = null,
      ratedAt = ZonedDateTime.parse(rating.rated_at),
      createdAt = nowUtc(),
      updatedAt = nowUtc(),
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

  fun toDatabaseShow(rating: RatingResultShow) =
    Rating(
      idTmdb = rating.show.ids.tmdb!!,
      type = "show",
      rating = rating.rating,
      seasonNumber = null,
      episodeNumber = null,
      ratedAt = ZonedDateTime.parse(rating.rated_at),
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

  fun toDatabaseEpisode(rating: RatingResultEpisode) =
    Rating(
      idTmdb = rating.episode.ids.tmdb!!,
      type = "episode",
      rating = rating.rating,
      seasonNumber = rating.episode.season,
      episodeNumber = rating.episode.number,
      ratedAt = ZonedDateTime.parse(rating.rated_at),
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

  fun toDatabaseSeason(rating: RatingResultSeason) =
    Rating(
      idTmdb = rating.season.ids.tmdb!!,
      type = "season",
      rating = rating.rating,
      seasonNumber = rating.season.number,
      episodeNumber = null,
      ratedAt = ZonedDateTime.parse(rating.rated_at),
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
