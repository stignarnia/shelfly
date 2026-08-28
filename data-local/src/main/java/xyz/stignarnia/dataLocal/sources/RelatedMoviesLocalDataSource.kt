package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.RelatedMovie

interface RelatedMoviesLocalDataSource {
  suspend fun insert(items: List<RelatedMovie>): List<Long>

  suspend fun getAllById(tmdbId: Long): List<RelatedMovie>

  suspend fun getAll(): List<RelatedMovie>

  suspend fun deleteById(tmdbId: Long)
}
