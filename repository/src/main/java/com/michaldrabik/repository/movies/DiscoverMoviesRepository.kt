package com.michaldrabik.repository.movies

import com.michaldrabik.common.Config
import com.michaldrabik.common.extensions.nowUtcMillis
import com.michaldrabik.data_local.LocalDataSource
import com.michaldrabik.data_local.database.model.DiscoverMovie
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
import com.michaldrabik.ui_model.Movie
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
      val genresQuery = genres.joinToString(",") { it.slug }

      val limit =
        if (showCollection) {
          TRAKT_DISCOVER_LIMIT
        } else {
          TRAKT_DISCOVER_LIMIT + (collectionSize / 2)
        }

      val trendingMoviesAsync = async {
        remoteSource.trakt
          .fetchTrendingMovies(genresQuery, limit)
          .map { mappers.movie.fromNetwork(it) }
      }

      val anticipatedMoviesAsync = async {
        remoteSource.trakt
          .fetchAnticipatedMovies(genresQuery, TRAKT_ANTICIPATED_LIMIT)
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

  private suspend fun loadRemotePopular(genres: List<Genre>): List<Movie> {
    val genresQuery = genres.joinToString(",") { it.slug }
    return remoteSource.trakt
      .fetchPopularMovies(genresQuery, TRAKT_DISCOVER_LIMIT)
      .map { mappers.movie.fromNetwork(it) }
  }

  private suspend fun loadRemoteAnticipated(genres: List<Genre>): List<Movie> {
    val genresQuery = genres.joinToString(",") { it.slug }
    return remoteSource.trakt
      .fetchAnticipatedMovies(genresQuery, TRAKT_DISCOVER_LIMIT)
      .map { mappers.movie.fromNetwork(it) }
  }

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
