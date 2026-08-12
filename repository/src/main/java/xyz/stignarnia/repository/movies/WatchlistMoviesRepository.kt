package xyz.stignarnia.repository.movies

import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_local.database.model.WatchlistMovie
import xyz.stignarnia.data_local.utilities.TransactionsProvider
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.ui_model.IdTmdb
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
