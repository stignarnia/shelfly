package xyz.stignarnia.ui_backup.features.sync

import timber.log.Timber
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.data_webdav.WebDavCredentials
import xyz.stignarnia.repository.settings.SettingsSyncRepository
import xyz.stignarnia.ui_backup.features.export.workers.BackupExportWorker
import xyz.stignarnia.ui_backup.features.import_.workers.BackupImportWorker
import xyz.stignarnia.ui_backup.features.sync.model.SyncPayload
import javax.inject.Inject

/**
 * One sync cycle, start to finish.
 *
 * Read local state, work out what this device has deleted since it last
 * published, take in every peer's state, merge, apply the result locally, and
 * publish what this device now holds.
 *
 * Sync is not backup, and this does not write snapshots. The scheduled job
 * still does that separately, on its own retention - otherwise the fork would
 * lose the property it was built for, since sync propagates an accidental
 * deletion rather than protecting against one.
 */
class SyncEngine @Inject internal constructor(
  private val exportWorker: BackupExportWorker,
  private val importWorker: BackupImportWorker,
  private val applier: SyncStateApplier,
  private val remoteSource: SyncRemoteSource,
  private val tombstoneStore: SyncTombstoneStore,
  private val settingsSyncRepository: SettingsSyncRepository,
) {

  data class Result(
    val deviceId: String,
    val peers: Int,
    val syncedAt: Long,
  )

  suspend fun sync(credentials: WebDavCredentials): Result {
    val deviceId = settingsSyncRepository.deviceId
    val startedAt = nowUtcMillis()
    Timber.i("Sync started as device $deviceId")

    val local = exportWorker.run()

    // What this device last published is the only baseline it has for spotting
    // its own deletions: anything in there and not here now is gone.
    val published = remoteSource.downloadOwn(credentials, deviceId)
    val tombstones = tombstoneStore.refresh(
      published = published?.state,
      local = local,
      // Dated at the previous sync, not now. All that is known is that the
      // deletion happened somewhere in that window, and the earlier bound lets
      // a genuine re-add elsewhere win the tie.
      deletedAt = settingsSyncRepository.lastSyncedAt,
      now = startedAt,
    )

    val peers = remoteSource.downloadPeers(credentials, deviceId)
    val merged = SyncMerge.merge(local = local, localTombstones = tombstones, peers = peers)

    // Re-publish peers' deletions as well as our own. Without this a deletion
    // would only ever be carried by the device that made it, and would leave
    // the network as soon as that device stopped syncing. A tombstone the merge
    // overruled is kept too - it is a claim, not a verdict, and it will keep
    // losing to the newer sighting until the store expires it.
    tombstoneStore.adopt(merged.tombstones)

    // Removals first: the importer only ever adds, so running it first would
    // re-add what the merge just decided is gone.
    applier.apply(local = local, merged = merged.state)
    importWorker.run(merged.state)

    // Publish what this device actually ended up with, not what the merge asked
    // for. The import can legitimately fall short - a show whose details will
    // not fetch is skipped - and this file is the baseline the next cycle diffs
    // against. Publishing the intent instead would make the next diff read
    // those gaps as deletions and propagate them to every other device.
    val applied = exportWorker.run()
    val syncedAt = nowUtcMillis()

    remoteSource
      .upload(
        credentials = credentials,
        payload = SyncPayload(
          deviceId = deviceId,
          updatedAt = syncedAt,
          state = applied,
          tombstones = merged.tombstones,
        ),
      ).getOrThrow()

    // Only once the upload has landed. A failed publish must not move the
    // baseline, or the deletions it was carrying would never be re-derived.
    settingsSyncRepository.lastSyncedAt = syncedAt

    Timber.i("Sync finished against ${peers.size} peer(s)")
    return Result(deviceId = deviceId, peers = peers.size, syncedAt = syncedAt)
  }
}
