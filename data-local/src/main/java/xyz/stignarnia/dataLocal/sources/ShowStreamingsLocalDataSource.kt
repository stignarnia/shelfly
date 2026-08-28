package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.ShowStreaming

interface ShowStreamingsLocalDataSource {
  suspend fun replace(
    tmdbId: Long,
    entities: List<ShowStreaming>,
  )

  suspend fun getById(tmdbId: Long): List<ShowStreaming>

  suspend fun deleteById(tmdbId: Long)

  suspend fun deleteAll()
}
