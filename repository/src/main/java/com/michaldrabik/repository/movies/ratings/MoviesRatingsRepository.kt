package com.michaldrabik.repository.movies.ratings

import com.michaldrabik.common.extensions.nowUtc
import com.michaldrabik.data_local.LocalDataSource
import com.michaldrabik.data_local.database.model.Rating
import com.michaldrabik.repository.mappers.Mappers
import com.michaldrabik.ui_model.Movie
import com.michaldrabik.ui_model.TraktRating
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoviesRatingsRepository @Inject constructor(
  val external: MoviesExternalRatingsRepository,
  private val localSource: LocalDataSource,
  private val mappers: Mappers,
) {

  companion object {
    private const val TYPE_MOVIE = "movie"
  }

  suspend fun loadMoviesRatings(): List<TraktRating> {
    val ratings = localSource.ratings.getAllByType(TYPE_MOVIE)
    return ratings.map {
      mappers.userRatings.fromDatabase(it)
    }
  }

  suspend fun loadRatings(movies: List<Movie>): List<TraktRating> {
    val ratings = mutableListOf<Rating>()
    movies.chunked(250).forEach { chunk ->
      val items = localSource.ratings.getAllByType(chunk.map { it.tmdbId }, TYPE_MOVIE)
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
    localSource.ratings.deleteByType(movie.tmdbId, TYPE_MOVIE)
  }
}
