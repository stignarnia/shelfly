package xyz.stignarnia.dataLocal.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import xyz.stignarnia.dataLocal.database.model.SyncTombstone
import xyz.stignarnia.dataLocal.sources.SyncTombstonesLocalDataSource

@Dao
interface SyncTombstonesDao : SyncTombstonesLocalDataSource {
  @Query("SELECT * FROM sync_tombstones")
  override suspend fun getAll(): List<SyncTombstone>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  override suspend fun upsert(tombstone: SyncTombstone)

  @Query("DELETE FROM sync_tombstones WHERE entity_type = :entityType AND entity_key = :entityKey")
  override suspend fun delete(
    entityType: String,
    entityKey: String,
  )

  @Query("DELETE FROM sync_tombstones WHERE deleted_at < :timestamp")
  override suspend fun deleteOlderThan(timestamp: Long)
}
