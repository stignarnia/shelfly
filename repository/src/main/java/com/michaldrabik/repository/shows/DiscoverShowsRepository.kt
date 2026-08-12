package com.michaldrabik.repository.shows

import com.michaldrabik.common.Config
import com.michaldrabik.common.extensions.nowUtcMillis
import com.michaldrabik.data_local.LocalDataSource
import com.michaldrabik.data_local.database.model.DiscoverShow
import com.michaldrabik.data_local.utilities.TransactionsProvider
import com.michaldrabik.data_remote.Config.TRAKT_ANTICIPATED_LIMIT
import com.michaldrabik.data_remote.Config.TRAKT_DISCOVER_LIMIT
import com.michaldrabik.data_remote.RemoteDataSource
import com.michaldrabik.repository.mappers.Mappers
import com.michaldrabik.ui_model.DiscoverFeed
import com.michaldrabik.ui_model.DiscoverFeed.ANTICIPATED
import com.michaldrabik.ui_model.DiscoverFeed.POPULAR
import com.michaldrabik.ui_model.DiscoverFeed.RECENT
import com.michaldrabik.ui_model.DiscoverFeed.TRENDING
import com.michaldrabik.ui_model.Genre
import com.michaldrabik.ui_model.Network
import com.michaldrabik.ui_model.Show
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class DiscoverShowsRepository @Inject constructor(
  private val remoteSource: RemoteDataSource,
  private val localSource: LocalDataSource,
  private val transactions: TransactionsProvider,
  private val mappers: Mappers,
) {

  suspend fun isCacheValid(): Boolean {
    val stamp = localSource.discoverShows.getMostRecent()?.createdAt ?: 0
    return nowUtcMillis() - stamp < Config.DISCOVER_SHOWS_CACHE_DURATION
  }

  suspend fun loadAllCached(): List<Show> {
    val cachedShows = localSource.discoverShows.getAll().map { it.idTmdb }
    val shows = localSource.shows.getAll(cachedShows)

    return cachedShows
      .map { id -> shows.first { it.idTmdb == id } }
      .map { mappers.show.fromDatabase(it) }
  }

  suspend fun loadAllRemote(
    order: DiscoverFeed,
    showCollection: Boolean,
    collectionSize: Int,
    genres: List<Genre>,
    networks: List<Network>,
  ): List<Show> =
    when (order) {
      TRENDING, RECENT -> loadRemoteTrending(genres, networks, showCollection, collectionSize)
      POPULAR -> loadRemotePopular(genres, networks)
      ANTICIPATED -> loadRemoteAnticipated(genres, networks)
    }

  private suspend fun loadRemoteTrending(
    genres: List<Genre>,
    networks: List<Network>,
    showCollection: Boolean,
    collectionSize: Int,
  ): List<Show> {
    return coroutineScope {
      val resultShows = mutableListOf<Show>()
      val genresQuery = genres.joinToString(",") { it.slug }
      val networksQuery = networks.joinToString(",") { it.channels.joinToString(",") }

      val limit =
        if (showCollection) {
          TRAKT_DISCOVER_LIMIT
        } else {
          TRAKT_DISCOVER_LIMIT + (collectionSize / 2)
        }

      val trendingShowsAsync = async {
        remoteSource.trakt
          .fetchTrendingShows(genresQuery, networksQuery, limit)
          .map { mappers.show.fromNetwork(it) }
      }

      val anticipatedShowsAsync = async {
        remoteSource.trakt
          .fetchAnticipatedShows(genresQuery, networksQuery, TRAKT_ANTICIPATED_LIMIT)
          .map { mappers.show.fromNetwork(it) }
      }

      val trendingShows = trendingShowsAsync.await()
      val anticipatedShows = anticipatedShowsAsync.await().toMutableList()

      trendingShows.forEachIndexed { index, trendingShow ->
        addIfMissing(resultShows, trendingShow)
        if (index != 0 && index % 6 == 0 && anticipatedShows.isNotEmpty()) {
          val anticipatedShow = anticipatedShows.removeAt(0)
          addIfMissing(resultShows, anticipatedShow)
        }
      }

      return@coroutineScope resultShows
    }
  }

  private suspend fun loadRemotePopular(
    genres: List<Genre>,
    networks: List<Network>,
  ): List<Show> {
    val genresQuery = genres.joinToString(",") { it.slug }
    val networksQuery = networks.joinToString(",") { it.channels.joinToString(",") }

    return remoteSource.trakt
      .fetchPopularShows(
        genres = genresQuery,
        networks = networksQuery,
        limit = TRAKT_DISCOVER_LIMIT,
      ).map { mappers.show.fromNetwork(it) }
  }

  private suspend fun loadRemoteAnticipated(
    genres: List<Genre>,
    networks: List<Network>,
  ): List<Show> {
    val genresQuery = genres.joinToString(",") { it.slug }
    val networksQuery = networks.joinToString(",") { it.channels.joinToString(",") }

    return remoteSource.trakt
      .fetchAnticipatedShows(
        genres = genresQuery,
        networks = networksQuery,
        limit = TRAKT_DISCOVER_LIMIT,
      ).map { mappers.show.fromNetwork(it) }
  }

  suspend fun cacheDiscoverShows(shows: List<Show>) {
    transactions.withTransaction {
      val timestamp = nowUtcMillis()
      localSource.shows.upsert(shows.map { mappers.show.toDatabase(it) })
      localSource.discoverShows.replace(
        shows.map {
          DiscoverShow(
            idTmdb = it.ids.tmdb.id,
            createdAt = timestamp,
            updatedAt = timestamp,
          )
        },
      )
    }
  }

  private fun addIfMissing(
    shows: MutableList<Show>,
    show: Show,
  ) {
    if (shows.any { it.ids.tmdb == show.ids.tmdb }) return
    shows.add(show)
  }
}
