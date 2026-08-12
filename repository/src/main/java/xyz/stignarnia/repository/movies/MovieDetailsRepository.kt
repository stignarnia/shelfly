package xyz.stignarnia.repository.movies

import xyz.stignarnia.common.Config
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_local.database.model.MoviesSyncLog
import xyz.stignarnia.data_remote.RemoteDataSource
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.ui_model.IdImdb
import xyz.stignarnia.ui_model.IdSlug
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Movie
import javax.inject.Inject

class MovieDetailsRepository @Inject constructor(
  private val remoteSource: RemoteDataSource,
  private val localSource: LocalDataSource,
  private val mappers: Mappers,
) {

  suspend fun load(
    idTmdb: IdTmdb,
    force: Boolean = false,
  ): Movie {
    val local = localSource.movies.getById(idTmdb.id)
    if (force || local == null || nowUtcMillis() - local.updatedAt > Config.MOVIE_DETAILS_CACHE_DURATION) {
      val remote = remoteSource.tmdb.fetchMovie(idTmdb.id)
      val movie = mappers.movie.fromNetwork(remote)
      localSource.movies.upsert(listOf(mappers.movie.toDatabase(movie)))
      localSource.moviesSyncLog.upsert(MoviesSyncLog(movie.tmdbId, nowUtcMillis()))
      return movie
    }
    return mappers.movie.fromDatabase(local)
  }

  suspend fun find(idImdb: IdImdb): Movie? {
    val localMovie = localSource.movies.getById(idImdb.id)
    if (localMovie != null) {
      return mappers.movie.fromDatabase(localMovie)
    }
    return null
  }

  suspend fun find(idTmdb: IdTmdb): Movie? {
    val localMovie = localSource.movies.getByTmdbId(idTmdb.id)
    if (localMovie != null) {
      return mappers.movie.fromDatabase(localMovie)
    }
    return null
  }

  suspend fun find(idSlug: IdSlug): Movie? {
    val localMovie = localSource.movies.getBySlug(idSlug.id)
    if (localMovie != null) {
      return mappers.movie.fromDatabase(localMovie)
    }
    return null
  }

  suspend fun delete(idTmdb: IdTmdb) = localSource.movies.deleteById(idTmdb.id)
}
