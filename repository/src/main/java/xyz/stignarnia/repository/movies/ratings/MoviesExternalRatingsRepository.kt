package xyz.stignarnia.repository.movies.ratings

import xyz.stignarnia.common.ConfigVariant
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.dataRemote.RemoteDataSource
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.Ratings
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoviesExternalRatingsRepository
  @Inject
  constructor(
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

      val remoteRatings =
        remoteSource.omdb
          .fetchOmdbData(movie.ids.imdb.id)
          .let { mappers.ratings.fromNetwork(it) }
          .copy(tmdb = Ratings.Value(String.format(Locale.ENGLISH, "%.1f", movie.rating), false))

      val dbRatings = mappers.ratings.toMovieDatabase(movie.ids.tmdb, remoteRatings)
      localSource.movieRatings.upsert(dbRatings)

      return remoteRatings
    }
  }
