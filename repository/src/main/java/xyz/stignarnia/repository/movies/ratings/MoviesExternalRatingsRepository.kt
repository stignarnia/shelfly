package xyz.stignarnia.repository.movies.ratings

import xyz.stignarnia.common.ConfigVariant
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_remote.RemoteDataSource
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.Ratings
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoviesExternalRatingsRepository @Inject constructor(
  private val remoteSource: RemoteDataSource,
  private val localSource: LocalDataSource,
  private val mappers: Mappers,
) {

  suspend fun loadRatings(movie: Movie): Ratings {
    val localRatings = localSource.movieRatings.getById(movie.tmdbId)
    localRatings?.let {
      if (nowUtcMillis() - it.updatedAt < ConfigVariant.RATINGS_CACHE_DURATION) {
        return mappers.ratings.fromDatabase(it)
      }
    }

    val remoteRatings = remoteSource.omdb
      .fetchOmdbData(movie.ids.imdb.id)
      .let { mappers.ratings.fromNetwork(it) }
      .copy(tmdb = Ratings.Value(String.format(Locale.ENGLISH, "%.1f", movie.rating), false))

    val dbRatings = mappers.ratings.toMovieDatabase(movie.ids.tmdb, remoteRatings)
    localSource.movieRatings.upsert(dbRatings)

    return remoteRatings
  }
}
