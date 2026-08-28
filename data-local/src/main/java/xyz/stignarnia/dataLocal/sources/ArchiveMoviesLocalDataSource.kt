package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.ArchiveMovie
import xyz.stignarnia.dataLocal.database.model.Movie

interface ArchiveMoviesLocalDataSource {
  suspend fun getAll(): List<Movie>

  suspend fun getAll(ids: List<Long>): List<Movie>

  suspend fun getAllTmdbIds(): List<Long>

  suspend fun getById(tmdbId: Long): Movie?

  suspend fun insert(movie: ArchiveMovie)

  suspend fun deleteById(tmdbId: Long)
}
