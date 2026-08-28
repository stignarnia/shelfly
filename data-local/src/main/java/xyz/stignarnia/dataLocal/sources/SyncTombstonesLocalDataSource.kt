package xyz.stignarnia.dataLocal.sources

import xyz.stignarnia.dataLocal.database.model.SyncTombstone

interface SyncTombstonesLocalDataSource {
  suspend fun getAll(): List<SyncTombstone>

  suspend fun upsert(tombstone: SyncTombstone)

  /**
   * Clears the tombstone for something the user has added back.
   * This device is authoritative about its own state, so the record is dropped rather than left for timestamp ordering to resolve.
   */
  suspend fun delete(
    entityType: String,
    entityKey: String,
  )

  /** Compaction: forgets deletions old enough that every device has seen them. */
  suspend fun deleteOlderThan(timestamp: Long)
}
