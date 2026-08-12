package com.michaldrabik.repository.movies

import com.michaldrabik.common.extensions.nowUtcMillis
import com.michaldrabik.data_local.LocalDataSource
import com.michaldrabik.data_local.database.model.ArchiveMovie
import com.michaldrabik.data_local.utilities.TransactionsProvider
import com.michaldrabik.repository.mappers.Mappers
import com.michaldrabik.ui_model.IdTmdb
import javax.inject.Inject

class HiddenMoviesRepository @Inject constructor(
  private val localSource: LocalDataSource,
  private val transactions: TransactionsProvider,
  private val mappers: Mappers,
) {

  suspend fun loadAll() =
    localSource.archiveMovies
      .getAll()
      .map { mappers.movie.fromDatabase(it) }

  suspend fun loadAll(ids: List<IdTmdb>) =
    localSource.archiveMovies
      .getAll(ids.map { it.id })
      .map { mappers.movie.fromDatabase(it) }

  suspend fun load(id: IdTmdb) =
    localSource.archiveMovies.getById(id.id)?.let {
      mappers.movie.fromDatabase(it)
    }

  suspend fun loadAllIds() = localSource.archiveMovies.getAllTraktIds()

  suspend fun insert(id: IdTmdb) {
    val dbMovie = ArchiveMovie.fromTmdbId(id.id, nowUtcMillis())
    transactions.withTransaction {
      with(localSource) {
        archiveMovies.insert(dbMovie)
        myMovies.deleteById(id.id)
        watchlistMovies.deleteById(id.id)
      }
    }
  }

  suspend fun delete(id: IdTmdb) = localSource.archiveMovies.deleteById(id.id)

  suspend fun exists(id: IdTmdb) = localSource.archiveMovies.getById(id.id) != null
}
