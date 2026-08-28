package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.Movie
import xyz.stignarnia.dataLocal.database.model.WatchlistMovie

interface WatchlistMoviesLocalDataSource {
  suspend fun getAll(): List<Movie>

  suspend fun getAllTmdbIds(): List<Long>

  suspend fun getById(tmdbId: Long): Movie?

  suspend fun insert(movie: WatchlistMovie)

  suspend fun deleteById(tmdbId: Long)

  suspend fun checkExists(tmdbId: Long): Boolean
}
