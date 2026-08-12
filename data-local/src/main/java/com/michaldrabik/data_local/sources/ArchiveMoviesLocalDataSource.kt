package com.michaldrabik.data_local.sources

import com.michaldrabik.data_local.database.model.ArchiveMovie
import com.michaldrabik.data_local.database.model.Movie

interface ArchiveMoviesLocalDataSource {

  suspend fun getAll(): List<Movie>

  suspend fun getAll(ids: List<Long>): List<Movie>

  suspend fun getAllTmdbIds(): List<Long>

  suspend fun getById(tmdbId: Long): Movie?

  suspend fun insert(movie: ArchiveMovie)

  suspend fun deleteById(tmdbId: Long)
}
