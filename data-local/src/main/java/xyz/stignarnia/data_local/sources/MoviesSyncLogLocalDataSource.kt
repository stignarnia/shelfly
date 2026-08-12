package xyz.stignarnia.data_local.sources

import xyz.stignarnia.data_local.database.model.MoviesSyncLog

interface MoviesSyncLogLocalDataSource {

  suspend fun getAll(): List<MoviesSyncLog>

  suspend fun upsert(log: MoviesSyncLog)
}
