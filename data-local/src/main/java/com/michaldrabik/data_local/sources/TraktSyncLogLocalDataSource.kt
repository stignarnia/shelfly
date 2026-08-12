package com.michaldrabik.data_local.sources

import com.michaldrabik.data_local.database.model.TraktSyncLog

interface TraktSyncLogLocalDataSource {

  suspend fun getAllShows(): List<TraktSyncLog>

  suspend fun insert(log: TraktSyncLog)

  suspend fun update(
    idTmdb: Long,
    type: String,
    syncedAt: Long,
  ): Int

  suspend fun deleteAll()

  suspend fun upsertShow(
    idTmdb: Long,
    syncedAt: Long,
  )
}
