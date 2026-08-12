package com.michaldrabik.repository.shows

import com.michaldrabik.common.Config
import com.michaldrabik.common.extensions.nowUtcMillis
import com.michaldrabik.data_local.LocalDataSource
import com.michaldrabik.data_local.database.model.RelatedShow
import com.michaldrabik.data_local.utilities.TransactionsProvider
import com.michaldrabik.data_remote.RemoteDataSource
import com.michaldrabik.repository.mappers.Mappers
import com.michaldrabik.ui_model.IdTmdb
import com.michaldrabik.ui_model.Show
import javax.inject.Inject

class RelatedShowsRepository @Inject constructor(
  private val remoteSource: RemoteDataSource,
  private val localSource: LocalDataSource,
  private val transactions: TransactionsProvider,
  private val mappers: Mappers,
) {

  suspend fun loadAll(
    show: Show,
    hiddenCount: Int,
  ): List<Show> {
    val relatedShows = localSource.relatedShows.getAllById(show.tmdbId)
    val latest = relatedShows.maxByOrNull { it.updatedAt }

    if (latest != null && nowUtcMillis() - latest.updatedAt < Config.RELATED_CACHE_DURATION) {
      val relatedShowsIds = relatedShows.map { it.idTmdb }
      return localSource.shows
        .getAll(relatedShowsIds)
        .map { mappers.show.fromDatabase(it) }
    }

    val remoteShows = remoteSource.tmdb
      .fetchRelatedShows(show.tmdbId)
      .map { mappers.show.fromNetwork(it) }

    cacheRelatedShows(remoteShows, show.ids.tmdb)

    return remoteShows
  }

  private suspend fun cacheRelatedShows(
    shows: List<Show>,
    showId: IdTmdb,
  ) {
    transactions.withTransaction {
      val timestamp = nowUtcMillis()
      localSource.shows.upsert(shows.map { mappers.show.toDatabase(it) })
      localSource.relatedShows.deleteById(showId.id)
      localSource.relatedShows.insert(
        shows.map {
          RelatedShow.fromTmdbId(it.ids.tmdb.id, showId.id, timestamp)
        },
      )
    }
  }
}
