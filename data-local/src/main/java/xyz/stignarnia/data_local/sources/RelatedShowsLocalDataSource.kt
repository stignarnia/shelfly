package xyz.stignarnia.data_local.sources

import xyz.stignarnia.data_local.database.model.RelatedShow

interface RelatedShowsLocalDataSource {

  suspend fun insert(items: List<RelatedShow>): List<Long>

  suspend fun getAllById(tmdbId: Long): List<RelatedShow>

  suspend fun getAll(): List<RelatedShow>

  suspend fun deleteById(tmdbId: Long)
}
