package com.michaldrabik.repository.movies

import com.michaldrabik.common.Config
import com.michaldrabik.common.extensions.nowUtcMillis
import com.michaldrabik.data_local.LocalDataSource
import com.michaldrabik.data_local.database.model.RelatedMovie
import com.michaldrabik.data_local.utilities.TransactionsProvider
import com.michaldrabik.data_remote.RemoteDataSource
import com.michaldrabik.repository.mappers.Mappers
import com.michaldrabik.ui_model.IdTmdb
import com.michaldrabik.ui_model.Movie
import javax.inject.Inject
import kotlin.math.min

class RelatedMoviesRepository @Inject constructor(
  private val remoteSource: RemoteDataSource,
  private val localSource: LocalDataSource,
  private val transactions: TransactionsProvider,
  private val mappers: Mappers,
) {

  suspend fun loadAll(movie: Movie): List<Movie> {
    val related = localSource.relatedMovies.getAllById(movie.ids.tmdb.id)
    val latest = related.maxByOrNull { it.updatedAt }

    if (latest != null && nowUtcMillis() - latest.updatedAt < Config.RELATED_CACHE_DURATION) {
      val relatedIds = related.map { it.idTmdb }
      return localSource.movies
        .getAll(relatedIds)
        .map { mappers.movie.fromDatabase(it) }
    }

    val remote = remoteSource.trakt
      .fetchRelatedMovies(movie.ids.tmdb.id, min(0, 15))
      .map { mappers.movie.fromNetwork(it) }

    cacheRelated(remote, movie.ids.tmdb)

    return remote
  }

  private suspend fun cacheRelated(
    movies: List<Movie>,
    movieId: IdTmdb,
  ) {
    transactions.withTransaction {
      val timestamp = nowUtcMillis()
      localSource.movies.upsert(movies.map { mappers.movie.toDatabase(it) })
      localSource.relatedMovies.deleteById(movieId.id)
      localSource.relatedMovies.insert(
        movies.map {
          RelatedMovie.fromTmdbId(it.ids.tmdb.id, movieId.id, timestamp)
        },
      )
    }
  }
}
