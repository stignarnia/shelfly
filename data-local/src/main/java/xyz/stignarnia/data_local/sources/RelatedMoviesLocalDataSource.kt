package xyz.stignarnia.data_local.sources

import xyz.stignarnia.data_local.database.model.RelatedMovie

interface RelatedMoviesLocalDataSource {

  suspend fun insert(items: List<RelatedMovie>): List<Long>

  suspend fun getAllById(tmdbId: Long): List<RelatedMovie>

  suspend fun getAll(): List<RelatedMovie>

  suspend fun deleteById(tmdbId: Long)
}
