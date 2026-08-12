package com.michaldrabik.repository.shows

import com.michaldrabik.common.extensions.nowUtcMillis
import com.michaldrabik.data_local.LocalDataSource
import com.michaldrabik.data_local.database.model.WatchlistShow
import com.michaldrabik.data_local.utilities.TransactionsProvider
import com.michaldrabik.repository.mappers.Mappers
import com.michaldrabik.ui_model.IdTmdb
import javax.inject.Inject

class WatchlistShowsRepository @Inject constructor(
  private val localSource: LocalDataSource,
  private val transactions: TransactionsProvider,
  private val mappers: Mappers,
) {

  suspend fun loadAll() =
    localSource.watchlistShows
      .getAll()
      .map { mappers.show.fromDatabase(it) }

  suspend fun loadAllIds() = localSource.watchlistShows.getAllTraktIds()

  suspend fun load(id: IdTmdb) =
    localSource.watchlistShows.getById(id.id)?.let {
      mappers.show.fromDatabase(it)
    }

  suspend fun insert(id: IdTmdb) {
    val dbShow = WatchlistShow.fromTmdbId(id.id, nowUtcMillis())
    with(localSource) {
      transactions.withTransaction {
        watchlistShows.insert(dbShow)
        myShows.deleteById(id.id)
        archiveShows.deleteById(id.id)
      }
    }
  }

  suspend fun delete(id: IdTmdb) = localSource.watchlistShows.deleteById(id.id)

  suspend fun exists(id: IdTmdb) = localSource.watchlistShows.checkExists(id.id)
}
