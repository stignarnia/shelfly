package xyz.stignarnia.repository.movies

import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_local.database.model.ArchiveMovie
import xyz.stignarnia.data_local.utilities.TransactionsProvider
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.ui_model.IdTmdb
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

  suspend fun loadAllIds() = localSource.archiveMovies.getAllTmdbIds()

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
