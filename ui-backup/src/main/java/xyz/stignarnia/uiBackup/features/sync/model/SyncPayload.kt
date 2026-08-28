package xyz.stignarnia.uiBackup.features.sync.model

import com.squareup.moshi.JsonClass
import xyz.stignarnia.uiBackup.model.BackupScheme

/**
 * What one device publishes: everything it currently has, plus every deletion it is still vouching for.
 *
 * A device only ever writes its own payload, which is what keeps concurrent syncs from conflicting - there is no shared object for two writers to race over, so no locking and no lost updates.
 */
@JsonClass(generateAdapter = true)
data class SyncPayload(
  val version: Int = SYNC_SCHEME_VERSION,
  val deviceId: String,
  val updatedAt: Long,
  val state: BackupScheme,
  val tombstones: List<SyncTombstoneEntry> = emptyList(),
)

const val SYNC_SCHEME_VERSION = 1
