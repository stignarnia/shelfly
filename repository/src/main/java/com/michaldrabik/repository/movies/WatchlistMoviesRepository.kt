package com.michaldrabik.repository.movies

import com.michaldrabik.common.extensions.nowUtcMillis
import com.michaldrabik.data_local.LocalDataSource
import com.michaldrabik.data_local.database.model.WatchlistMovie
import com.michaldrabik.data_local.utilities.TransactionsProvider
import com.michaldrabik.repository.mappers.Mappers
import com.michaldrabik.ui_model.IdTmdb
import javax.inject.Inject

class WatchlistMoviesRepository @Inject constructor(
  private val localSource: LocalDataSource,
  private val transactions: TransactionsProvider,
  private val mappers: Mappers,
) {

  suspend fun loadAll() =
    localSource.watchlistMovies
      .getAll()
      .map { mappers.movie.fromDatabase(it) }

  suspend fun loadAllIds() = localSource.watchlistMovies.getAllTmdbIds()

  suspend fun load(id: IdTmdb) =
    localSource.watchlistMovies.getById(id.id)?.let {
      mappers.movie.fromDatabase(it)
    }

  suspend fun insert(id: IdTmdb) {
    val movie = WatchlistMovie.fromTmdbId(id.id, nowUtcMillis())
    transactions.withTransaction {
      with(localSource) {
        watchlistMovies.insert(movie)
        myMovies.deleteById(movie.idTmdb)
        archiveMovies.deleteById(movie.idTmdb)
      }
    }
  }

  suspend fun delete(id: IdTmdb) = localSource.watchlistMovies.deleteById(id.id)

  suspend fun exists(id: IdTmdb) = localSource.watchlistMovies.checkExists(id.id)
}
