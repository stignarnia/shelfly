package xyz.stignarnia.repository.shows

import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.dataLocal.database.model.ArchiveShow
import xyz.stignarnia.dataLocal.utilities.TransactionsProvider
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.uiModel.IdTmdb
import javax.inject.Inject

class HiddenShowsRepository
  @Inject
  constructor(
    private val localSource: LocalDataSource,
    private val transactions: TransactionsProvider,
    private val mappers: Mappers,
  ) {
    suspend fun loadAll() =
      localSource.archiveShows
        .getAll()
        .map { mappers.show.fromDatabase(it) }

    suspend fun loadAll(ids: List<IdTmdb>) =
      localSource.archiveShows
        .getAll(ids.map { it.id })
        .map { mappers.show.fromDatabase(it) }

    suspend fun load(id: IdTmdb) =
      localSource.archiveShows.getById(id.id)?.let {
        mappers.show.fromDatabase(it)
      }

    suspend fun loadAllIds() = localSource.archiveShows.getAllTmdbIds()

    suspend fun insert(id: IdTmdb) {
      val dbShow = ArchiveShow.fromTmdbId(id.id, nowUtcMillis())
      with(localSource) {
        transactions.withTransaction {
          archiveShows.insert(dbShow)
          myShows.deleteById(id.id)
          watchlistShows.deleteById(id.id)
        }
      }
    }

    suspend fun delete(id: IdTmdb) = localSource.archiveShows.deleteById(id.id)

    suspend fun exists(id: IdTmdb) = localSource.archiveShows.getById(id.id) != null
  }
