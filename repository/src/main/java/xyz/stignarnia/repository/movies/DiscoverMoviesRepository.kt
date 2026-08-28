package xyz.stignarnia.repository.movies

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.dataLocal.LocalDataSource
import xyz.stignarnia.dataLocal.database.model.DiscoverMovie
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
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.StreamingProvider
import javax.inject.Inject

class DiscoverMoviesRepository
  @Inject
  constructor(
    private val remoteSource: RemoteDataSource,
    private val localSource: LocalDataSource,
    private val transactions: TransactionsProvider,
    private val mappers: Mappers,
  ) {
    suspend fun isCacheValid(): Boolean {
      val stamp = localSource.discoverMovies.getMostRecent()?.createdAt ?: 0
      return nowUtcMillis() - stamp < Config.DISCOVER_MOVIES_CACHE_DURATION
    }

    suspend fun loadAllCached(): List<Movie> {
      val cachedMovies = localSource.discoverMovies.getAll().map { it.idTmdb }
      val movies = localSource.movies.getAll(cachedMovies)

      return cachedMovies
        .map { id -> movies.first { it.idTmdb == id } }
        .map { mappers.movie.fromDatabase(it) }
    }

    suspend fun loadAllRemote(
      order: DiscoverFeed,
      showCollection: Boolean,
      collectionSize: Int,
      genres: List<Genre>,
      providers: List<StreamingProvider>,
      countryCode: String,
    ): List<Movie> =
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
    ): List<Movie> {
      return coroutineScope {
        val resultMovies = mutableListOf<Movie>()

        val limit =
          if (showCollection) {
            DISCOVER_LIMIT
          } else {
            DISCOVER_LIMIT + (collectionSize / 2)
          }

        val trendingMoviesAsync =
          async {
            remoteSource.tmdb
              .fetchTrendingMovies(genres.map { it.slug }, providers.map { it.id }, countryCode, limit)
              .map { mappers.movie.fromNetwork(it) }
          }

        val anticipatedMoviesAsync =
          async {
            remoteSource.tmdb
              .fetchAnticipatedMovies(genres.map { it.slug }, providers.map { it.id }, countryCode, ANTICIPATED_LIMIT)
              .map { mappers.movie.fromNetwork(it) }
          }

        val trendingMovies = trendingMoviesAsync.await()
        val anticipatedMovies = anticipatedMoviesAsync.await().toMutableList()

        trendingMovies.forEachIndexed { index, trendingMovie ->
          addIfMissing(resultMovies, trendingMovie)
          if (index != 0 && index % 6 == 0 && anticipatedMovies.isNotEmpty()) {
            val anticipatedMovie = anticipatedMovies.removeAt(0)
            addIfMissing(resultMovies, anticipatedMovie)
          }
        }

        return@coroutineScope resultMovies
      }
    }

    private suspend fun loadRemotePopular(
      genres: List<Genre>,
      providers: List<StreamingProvider>,
      countryCode: String,
    ): List<Movie> =
      remoteSource.tmdb
        .fetchPopularMovies(genres.map { it.slug }, providers.map { it.id }, countryCode, DISCOVER_LIMIT)
        .map { mappers.movie.fromNetwork(it) }

    private suspend fun loadRemoteAnticipated(
      genres: List<Genre>,
      providers: List<StreamingProvider>,
      countryCode: String,
    ): List<Movie> =
      remoteSource.tmdb
        .fetchAnticipatedMovies(genres.map { it.slug }, providers.map { it.id }, countryCode, DISCOVER_LIMIT)
        .map { mappers.movie.fromNetwork(it) }

    suspend fun cacheDiscoverMovies(movies: List<Movie>) {
      transactions.withTransaction {
        val timestamp = nowUtcMillis()
        localSource.movies.upsert(movies.map { mappers.movie.toDatabase(it) })
        localSource.discoverMovies.replace(
          movies.map {
            DiscoverMovie(
              idTmdb = it.ids.tmdb.id,
              createdAt = timestamp,
              updatedAt = timestamp,
            )
          },
        )
      }
    }

    private fun addIfMissing(
      movies: MutableList<Movie>,
      movie: Movie,
    ) {
      if (movies.any { it.ids.tmdb == movie.ids.tmdb }) {
        return
      }
      movies.add(movie)
    }
  }
