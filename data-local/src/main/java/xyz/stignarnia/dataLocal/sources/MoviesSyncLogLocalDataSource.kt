package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.MoviesSyncLog

interface MoviesSyncLogLocalDataSource {
  suspend fun getAll(): List<MoviesSyncLog>

  suspend fun upsert(log: MoviesSyncLog)
}
