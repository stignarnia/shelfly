package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.MovieStreaming

interface MovieStreamingsLocalDataSource {
  suspend fun replace(
    tmdbId: Long,
    entities: List<MovieStreaming>,
  )

  suspend fun getById(tmdbId: Long): List<MovieStreaming>

  suspend fun deleteById(tmdbId: Long)

  suspend fun deleteAll()
}
