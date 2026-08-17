package xyz.stignarnia.data_local.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * A record that the user deleted something, kept so the deletion can be told apart from "this device never had it".
 *
 * Without these, syncing is a union and deletions cannot survive a round trip: unfollow a show here, and the next device to sync adds it straight back.
 *
 * [entityKey] identifies the thing within its [entityType] - a TMDB id for a collection entry, a composite for an episode or list item.
 * The pair is the primary key, so deleting the same thing twice updates the timestamp rather than accumulating rows.
 */
@Entity(tableName = "sync_tombstones", primaryKeys = ["entity_type", "entity_key"])
data class SyncTombstone(
  @ColumnInfo(name = "entity_type") val entityType: String,
  @ColumnInfo(name = "entity_key") val entityKey: String,
  @ColumnInfo(name = "deleted_at") val deletedAt: Long,
)
