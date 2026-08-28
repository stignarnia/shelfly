package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.EpisodesSyncLog

interface EpisodesSyncLogLocalDataSource {
  suspend fun getAll(): List<EpisodesSyncLog>

  suspend fun upsert(log: EpisodesSyncLog)
}
