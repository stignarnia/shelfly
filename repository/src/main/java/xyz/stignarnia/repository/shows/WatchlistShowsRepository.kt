package xyz.stignarnia.repository.shows

import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.dataLocal.database.model.WatchlistShow
import xyz.stignarnia.dataLocal.utilities.TransactionsProvider
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.uiModel.IdTmdb
import javax.inject.Inject

class WatchlistShowsRepository
  @Inject
  constructor(
    private val localSource: LocalDataSource,
    private val transactions: TransactionsProvider,
    private val mappers: Mappers,
  ) {
    suspend fun loadAll() =
      localSource.watchlistShows
        .getAll()
        .map { mappers.show.fromDatabase(it) }

    suspend fun loadAllIds() = localSource.watchlistShows.getAllTmdbIds()

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
