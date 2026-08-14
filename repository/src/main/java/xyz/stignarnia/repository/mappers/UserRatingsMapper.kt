package xyz.stignarnia.repository.mappers

import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.data_local.database.model.Rating
import xyz.stignarnia.ui_model.Episode
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.Season
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_model.UserRating
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Season and episode ratings are stored under the show they belong to, not
 * under their own TMDB id - see [Rating]. That id is not part of a season or
 * episode's identity anywhere outside the database, so the show has to be
 * passed in.
 */
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
    type = Rating.TYPE_MOVIE,
    rating = rating,
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
    type = Rating.TYPE_SHOW,
    rating = rating,
    ratedAt = ratedAt,
    createdAt = nowUtc(),
    updatedAt = nowUtc(),
  )

  fun toDatabaseEpisode(
    showId: IdTmdb,
    episode: Episode,
    rating: Int,
    ratedAt: ZonedDateTime,
  ) = Rating(
    idTmdb = showId.id,
    type = Rating.TYPE_EPISODE,
    rating = rating,
    seasonNumber = episode.season,
    episodeNumber = episode.number,
    ratedAt = ratedAt,
    createdAt = nowUtc(),
    updatedAt = nowUtc(),
  )

  fun toDatabaseSeason(
    showId: IdTmdb,
    season: Season,
    rating: Int,
    ratedAt: ZonedDateTime,
  ) = Rating(
    idTmdb = showId.id,
    type = Rating.TYPE_SEASON,
    rating = rating,
    seasonNumber = season.number,
    ratedAt = ratedAt,
    createdAt = nowUtc(),
    updatedAt = nowUtc(),
  )
}
