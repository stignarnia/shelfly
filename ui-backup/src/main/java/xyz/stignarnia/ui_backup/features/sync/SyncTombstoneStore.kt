package xyz.stignarnia.ui_backup.features.sync

import xyz.stignarnia.data_local.LocalDataSource
import xyz.stignarnia.data_local.database.model.SyncTombstone
import xyz.stignarnia.repository.settings.SettingsSyncRepository
import xyz.stignarnia.ui_backup.features.sync.model.SyncEntity
import xyz.stignarnia.ui_backup.features.sync.model.SyncTombstoneEntry
import xyz.stignarnia.ui_backup.model.BackupScheme
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Accumulates this device's deletions and keeps them alive long enough for every peer to hear about them.
 *
 * A deletion derived from the diff is only visible for the cycle that found it.
 * If it were published once and dropped, a device that happened to be offline during that single sync would never see it, and would re-add the item on its next merge - the deletion silently undone by a peer that simply was not listening at the right moment.
 *
 * So derived deletions are persisted and re-published every cycle until they expire.
 * The window is the price of that guarantee: a device offline for longer than [SettingsSyncRepository.TOMBSTONE_RETENTION_DAYS] may still resurrect what it never learned was deleted.
 */
internal class SyncTombstoneStore @Inject constructor(
  private val localSource: LocalDataSource,
) {

  /**
   * Brings the store up to date for this cycle and returns everything still worth publishing.
   *
   * @param published what this device last uploaded, or null if it never has.
   * @param local current state.
   * @param deletedAt the timestamp to date new deletions with - the previous
   * sync time, being the earliest moment they could have happened.
   * @param now used to expire old tombstones.
   */
  suspend fun refresh(
    published: BackupScheme?,
    local: BackupScheme,
    deletedAt: Long,
    now: Long,
  ): List<SyncTombstoneEntry> {
    SyncTombstoneDeriver
      .derive(published, local, deletedAt)
      .forEach { entry ->
        localSource.syncTombstones.upsert(
          SyncTombstone(
            entityType = entry.entity.name,
            entityKey = entry.key,
            deletedAt = entry.deletedAt,
          ),
        )
      }

    // Anything present again was re-added here since it was deleted.
    // This device is authoritative about its own state, so the record goes rather than being left for timestamp ordering to settle.
    val presentLocally = SyncStateFlattener.flatten(local).keys
    presentLocally.forEach { (entity, key) ->
      localSource.syncTombstones.delete(entity.name, key)
    }

    val expiry = now - TimeUnit.DAYS.toMillis(SettingsSyncRepository.TOMBSTONE_RETENTION_DAYS)
    localSource.syncTombstones.deleteOlderThan(expiry)

    return localSource.syncTombstones
      .getAll()
      .mapNotNull { stored ->
        val entity = runCatching { SyncEntity.valueOf(stored.entityType) }.getOrNull()
          ?: return@mapNotNull null
        SyncTombstoneEntry(entity = entity, key = stored.entityKey, deletedAt = stored.deletedAt)
      }
  }

  /**
   * Records deletions learned from a peer, so this device re-publishes them too.
   * Without this a deletion would only ever be carried by the device that made it, and would vanish from the network the moment that device stopped syncing or its window expired.
   */
  suspend fun adopt(entries: List<SyncTombstoneEntry>) {
    entries.forEach { entry ->
      localSource.syncTombstones.upsert(
        SyncTombstone(
          entityType = entry.entity.name,
          entityKey = entry.key,
          deletedAt = entry.deletedAt,
        ),
      )
    }
  }
}
