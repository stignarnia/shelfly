package xyz.stignarnia.data_local.sources

import xyz.stignarnia.data_local.database.model.Movie
import xyz.stignarnia.data_local.database.model.WatchlistMovie

interface WatchlistMoviesLocalDataSource {

  suspend fun getAll(): List<Movie>

  suspend fun getAllTmdbIds(): List<Long>

  suspend fun getById(tmdbId: Long): Movie?

  suspend fun insert(movie: WatchlistMovie)

  suspend fun deleteById(tmdbId: Long)

  suspend fun checkExists(tmdbId: Long): Boolean
}
