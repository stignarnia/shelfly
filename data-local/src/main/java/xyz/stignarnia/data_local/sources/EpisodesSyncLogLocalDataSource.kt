package xyz.stignarnia.data_local.sources

import xyz.stignarnia.data_local.database.model.EpisodesSyncLog

interface EpisodesSyncLogLocalDataSource {

  suspend fun getAll(): List<EpisodesSyncLog>

  suspend fun upsert(log: EpisodesSyncLog)
}
