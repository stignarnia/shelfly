package xyz.stignarnia.repository.shows

import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.data_local.database.model.MyShow
import xyz.stignarnia.data_local.sources.ArchiveShowsLocalDataSource
import xyz.stignarnia.data_local.sources.MyShowsLocalDataSource
import xyz.stignarnia.data_local.sources.WatchlistShowsLocalDataSource
import xyz.stignarnia.data_local.utilities.TransactionsProvider
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.ui_model.IdTmdb
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
