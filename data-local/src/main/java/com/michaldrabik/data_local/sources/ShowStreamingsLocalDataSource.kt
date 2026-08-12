package com.michaldrabik.data_local.sources

import com.michaldrabik.data_local.database.model.ShowStreaming

interface ShowStreamingsLocalDataSource {

  suspend fun replace(
    tmdbId: Long,
    entities: List<ShowStreaming>,
  )

  suspend fun getById(tmdbId: Long): List<ShowStreaming>

  suspend fun deleteById(tmdbId: Long)

  suspend fun deleteAll()
}
