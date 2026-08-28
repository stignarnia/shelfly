package xyz.stignarnia.repository.shows

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.dataLocal.database.model.DiscoverShow
import xyz.stignarnia.dataLocal.utilities.TransactionsProvider
import xyz.stignarnia.dataRemote.Config.ANTICIPATED_LIMIT
import xyz.stignarnia.dataRemote.Config.DISCOVER_LIMIT
import xyz.stignarnia.dataRemote.RemoteDataSource
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.uiModel.DiscoverFeed
import xyz.stignarnia.uiModel.DiscoverFeed.ANTICIPATED
import xyz.stignarnia.uiModel.DiscoverFeed.POPULAR
import xyz.stignarnia.uiModel.DiscoverFeed.RECENT
import xyz.stignarnia.uiModel.DiscoverFeed.TRENDING
import xyz.stignarnia.uiModel.Genre
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.StreamingProvider
import javax.inject.Inject

class DiscoverShowsRepository
  @Inject
  constructor(
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
      providers: List<StreamingProvider>,
      countryCode: String,
    ): List<Show> =
      when (order) {
        TRENDING, RECENT -> loadRemoteTrending(genres, providers, countryCode, showCollection, collectionSize)
        POPULAR -> loadRemotePopular(genres, providers, countryCode)
        ANTICIPATED -> loadRemoteAnticipated(genres, providers, countryCode)
      }

    private suspend fun loadRemoteTrending(
      genres: List<Genre>,
      providers: List<StreamingProvider>,
      countryCode: String,
      showCollection: Boolean,
      collectionSize: Int,
    ): List<Show> {
      return coroutineScope {
        val resultShows = mutableListOf<Show>()

        val limit =
          if (showCollection) {
            DISCOVER_LIMIT
          } else {
            DISCOVER_LIMIT + (collectionSize / 2)
          }

        val trendingShowsAsync =
          async {
            remoteSource.tmdb
              .fetchTrendingShows(
                genres = genres.map { it.slug },
                providers = providers.map { it.id },
                countryCode = countryCode,
                limit = limit,
              ).map { mappers.show.fromNetwork(it) }
          }

        val anticipatedShowsAsync =
          async {
            remoteSource.tmdb
              .fetchAnticipatedShows(
                genres = genres.map { it.slug },
                providers = providers.map { it.id },
                countryCode = countryCode,
                limit = ANTICIPATED_LIMIT,
              ).map { mappers.show.fromNetwork(it) }
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
      providers: List<StreamingProvider>,
      countryCode: String,
    ): List<Show> =
      remoteSource.tmdb
        .fetchPopularShows(
          genres = genres.map { it.slug },
          providers = providers.map { it.id },
          countryCode = countryCode,
          limit = DISCOVER_LIMIT,
        ).map { mappers.show.fromNetwork(it) }

    private suspend fun loadRemoteAnticipated(
      genres: List<Genre>,
      providers: List<StreamingProvider>,
      countryCode: String,
    ): List<Show> =
      remoteSource.tmdb
        .fetchAnticipatedShows(
          genres = genres.map { it.slug },
          providers = providers.map { it.id },
          countryCode = countryCode,
          limit = DISCOVER_LIMIT,
        ).map { mappers.show.fromNetwork(it) }

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
