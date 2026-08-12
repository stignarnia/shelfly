package com.michaldrabik.data_local.sources

import com.michaldrabik.data_local.database.model.MovieStreaming

interface MovieStreamingsLocalDataSource {

  suspend fun replace(
    tmdbId: Long,
    entities: List<MovieStreaming>,
  )

  suspend fun getById(tmdbId: Long): List<MovieStreaming>

  suspend fun deleteById(tmdbId: Long)

  suspend fun deleteAll()
}
