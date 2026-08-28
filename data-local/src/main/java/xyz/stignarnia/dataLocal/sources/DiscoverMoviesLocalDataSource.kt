package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.DiscoverMovie

interface DiscoverMoviesLocalDataSource {
  suspend fun getAll(): List<DiscoverMovie>

  suspend fun getMostRecent(): DiscoverMovie?

  suspend fun upsert(movies: List<DiscoverMovie>)

  suspend fun deleteAll()

  suspend fun replace(movies: List<DiscoverMovie>)
}
