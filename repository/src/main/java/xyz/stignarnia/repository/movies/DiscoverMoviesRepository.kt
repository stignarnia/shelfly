package xyz.stignarnia.repository.movies

import xyz.stignarnia.common.Config
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_local.database.model.DiscoverMovie
import xyz.stignarnia.data_local.utilities.TransactionsProvider
import xyz.stignarnia.data_remote.Config.ANTICIPATED_LIMIT
import xyz.stignarnia.data_remote.Config.DISCOVER_LIMIT
import xyz.stignarnia.data_remote.RemoteDataSource
import xyz.stignarnia.repository.mappers.Mappers
import xyz.stignarnia.ui_model.DiscoverFeed
import xyz.stignarnia.ui_model.DiscoverFeed.ANTICIPATED
import xyz.stignarnia.ui_model.DiscoverFeed.POPULAR
import xyz.stignarnia.ui_model.DiscoverFeed.RECENT
import xyz.stignarnia.ui_model.DiscoverFeed.TRENDING
import xyz.stignarnia.ui_model.Genre
import xyz.stignarnia.ui_model.Movie
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class DiscoverMoviesRepository @Inject constructor(
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
  ): List<Movie> =
    when (order) {
      TRENDING, RECENT -> loadRemoteTrending(genres, showCollection, collectionSize)
      POPULAR -> loadRemotePopular(genres)
      ANTICIPATED -> loadRemoteAnticipated(genres)
    }

  private suspend fun loadRemoteTrending(
    genres: List<Genre>,
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

      val trendingMoviesAsync = async {
        remoteSource.tmdb
          .fetchTrendingMovies(genres.map { it.slug }, limit)
          .map { mappers.movie.fromNetwork(it) }
      }

      val anticipatedMoviesAsync = async {
        remoteSource.tmdb
          .fetchAnticipatedMovies(genres.map { it.slug }, ANTICIPATED_LIMIT)
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

  private suspend fun loadRemotePopular(genres: List<Genre>): List<Movie> =
    remoteSource.tmdb
      .fetchPopularMovies(genres.map { it.slug }, DISCOVER_LIMIT)
      .map { mappers.movie.fromNetwork(it) }

  private suspend fun loadRemoteAnticipated(genres: List<Genre>): List<Movie> =
    remoteSource.tmdb
      .fetchAnticipatedMovies(genres.map { it.slug }, DISCOVER_LIMIT)
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
