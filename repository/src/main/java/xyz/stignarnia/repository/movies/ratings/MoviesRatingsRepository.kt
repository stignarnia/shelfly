package xyz.stignarnia.repository.movies.ratings

import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_local.database.model.Rating
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.UserRating
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoviesRatingsRepository @Inject constructor(
  val external: MoviesExternalRatingsRepository,
  private val localSource: LocalDataSource,
  private val mappers: Mappers,
) {

  suspend fun loadMoviesRatings(): List<UserRating> {
    val ratings = localSource.ratings.getAllByType(Rating.TYPE_MOVIE)
    return ratings.map {
      mappers.userRatings.fromDatabase(it)
    }
  }

  suspend fun loadRatings(movies: List<Movie>): List<UserRating> {
    val ratings = mutableListOf<Rating>()
    movies.chunked(250).forEach { chunk ->
      val items = localSource.ratings.getAllByType(chunk.map { it.tmdbId }, Rating.TYPE_MOVIE)
      ratings.addAll(items)
    }
    return ratings.map {
      mappers.userRatings.fromDatabase(it)
    }
  }

  suspend fun addRating(
    movie: Movie,
    rating: Int,
  ) {
    val ratedAt = nowUtc()
    val entity = mappers.userRatings.toDatabaseMovie(movie, rating, ratedAt)
    localSource.ratings.replace(entity)
  }

  suspend fun deleteRating(movie: Movie) {
    localSource.ratings.deleteByKey(
      tmdbId = movie.tmdbId,
      type = Rating.TYPE_MOVIE,
      seasonNumber = Rating.NO_NUMBER,
      episodeNumber = Rating.NO_NUMBER,
    )
  }
}
