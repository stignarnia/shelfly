package xyz.stignarnia.repository.shows

import xyz.stignarnia.common.Config
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.dataLocal.utilities.TransactionsProvider
import xyz.stignarnia.dataRemote.RemoteDataSource
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.uiModel.IdImdb
import xyz.stignarnia.uiModel.IdSlug
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Show
import javax.inject.Inject

class ShowDetailsRepository
  @Inject
  constructor(
    private val remoteSource: RemoteDataSource,
    private val localSource: LocalDataSource,
    private val transactions: TransactionsProvider,
    private val mappers: Mappers,
  ) {
    suspend fun load(
      idTmdb: IdTmdb,
      force: Boolean = false,
    ): Show {
      val localShow = localSource.shows.getById(idTmdb.id)
      // Only the details endpoint appends external_ids, so a row first cached by a list endpoint carries no IMDb id.
      // Treat that as stale however fresh it is, otherwise external ratings have nothing to look up for the whole cache window.
      // Shows TMDB has no IMDb id for simply refetch on each open.
      val isMissingImdbId = localShow != null && localShow.idImdb.isBlank()
      if (force ||
        localShow == null ||
        isMissingImdbId ||
        nowUtcMillis() - localShow.updatedAt > Config.SHOW_DETAILS_CACHE_DURATION
      ) {
        val remoteShow = remoteSource.tmdb.fetchShow(idTmdb.id)
        val show = mappers.show.fromNetwork(remoteShow)
        localSource.shows.upsert(listOf(mappers.show.toDatabase(show)))
        return show
      }
      return mappers.show.fromDatabase(localShow)
    }

    suspend fun find(idImdb: IdImdb): Show? {
      val localShow = localSource.shows.getById(idImdb.id)
      if (localShow != null) {
        return mappers.show.fromDatabase(localShow)
      }
      return null
    }

    suspend fun find(idTmdb: IdTmdb): Show? {
      val localShow = localSource.shows.getByTmdbId(idTmdb.id)
      if (localShow != null) {
        return mappers.show.fromDatabase(localShow)
      }
      return null
    }

    suspend fun find(idSlug: IdSlug): Show? {
      val localShow = localSource.shows.getBySlug(idSlug.id)
      if (localShow != null) {
        return mappers.show.fromDatabase(localShow)
      }
      return null
    }

    suspend fun delete(idTmdb: IdTmdb) {
      with(localSource) {
        transactions.withTransaction {
          shows.deleteById(idTmdb.id)
          seasons.deleteAllForShow(idTmdb.id)
          episodes.deleteAllForShow(idTmdb.id)
        }
      }
    }
  }
