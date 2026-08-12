package com.michaldrabik.repository.shows

import com.michaldrabik.common.extensions.nowUtcMillis
import com.michaldrabik.data_local.database.model.MyShow
import com.michaldrabik.data_local.sources.ArchiveShowsLocalDataSource
import com.michaldrabik.data_local.sources.MyShowsLocalDataSource
import com.michaldrabik.data_local.sources.WatchlistShowsLocalDataSource
import com.michaldrabik.data_local.utilities.TransactionsProvider
import com.michaldrabik.repository.mappers.Mappers
import com.michaldrabik.ui_model.IdTmdb
import javax.inject.Inject

class MyShowsRepository @Inject constructor(
  private val myShowsLocalSource: MyShowsLocalDataSource,
  private val watchlistShowsLocalSource: WatchlistShowsLocalDataSource,
  private val hiddenShowsLocalDataSource: ArchiveShowsLocalDataSource,
  private val transactions: TransactionsProvider,
  private val mappers: Mappers,
) {

  suspend fun load(id: IdTmdb) =
    myShowsLocalSource.getById(id.id)?.let {
      mappers.show.fromDatabase(it)
    }

  suspend fun loadAll() =
    myShowsLocalSource
      .getAll()
      .map { mappers.show.fromDatabase(it) }

  suspend fun loadAll(ids: List<IdTmdb>) =
    myShowsLocalSource
      .getAll(ids.map { it.id })
      .map { mappers.show.fromDatabase(it) }

  suspend fun loadAllRecent(amount: Int) =
    myShowsLocalSource
      .getAllRecent(amount)
      .map { mappers.show.fromDatabase(it) }

  suspend fun loadAllIds() = myShowsLocalSource.getAllTmdbIds()

  suspend fun insert(
    id: IdTmdb,
    lastWatchedAt: Long,
  ) {
    val nowUtc = nowUtcMillis()
    val dbShow = MyShow.fromTmdbId(
      tmdbId = id.id,
      createdAt = nowUtc,
      updatedAt = nowUtc,
      watchedAt = lastWatchedAt,
    )
    transactions.withTransaction {
      myShowsLocalSource.insert(listOf(dbShow))
      watchlistShowsLocalSource.deleteById(id.id)
      hiddenShowsLocalDataSource.deleteById(id.id)
    }
  }

  suspend fun delete(id: IdTmdb) {
    myShowsLocalSource.deleteById(id.id)
  }

  suspend fun exists(id: IdTmdb) = myShowsLocalSource.checkExists(id.id)

  suspend fun updateWatchedAt(
    idTmdb: Long,
    watchedAt: Long,
  ) {
    myShowsLocalSource.updateWatchedAt(idTmdb, watchedAt)
  }
}
