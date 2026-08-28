package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.Movie
import xyz.stignarnia.dataLocal.database.model.MovieSearch

interface MoviesLocalDataSource {
  suspend fun getAll(): List<Movie>

  suspend fun getAllForSearch(): List<MovieSearch>

  suspend fun getAll(ids: List<Long>): List<Movie>

  suspend fun getAllTmdbIds(tmdbIds: List<Long>): Map<Long, Long>

  suspend fun getAllChunked(ids: List<Long>): List<Movie>

  suspend fun getById(tmdbId: Long): Movie?

  suspend fun getByTmdbId(tmdbId: Long): Movie?

  suspend fun getBySlug(slug: String): Movie?

  suspend fun getById(imdbId: String): Movie?

  suspend fun deleteById(tmdbId: Long)

  suspend fun upsert(movies: List<Movie>)
}
