package xyz.stignarnia.ui_backup.features.sync

/**
 * Names the one file each device owns in the backup directory.
 *
 * Sync files sit alongside the timestamped snapshots and are deliberately
 * distinguishable from them: snapshots are point-in-time copies kept under
 * retention, sync files are live state that each device overwrites. Confusing
 * the two would either prune live state or restore a peer's file as a backup.
 */
internal object SyncFileName {

  const val PREFIX = "shelfly_sync_"
  const val FILE_TYPE = ".json"

  fun forDevice(deviceId: String) = "$PREFIX$deviceId$FILE_TYPE"

  fun isSyncFile(name: String) = name.startsWith(PREFIX) && name.endsWith(FILE_TYPE)

  fun deviceIdOf(name: String) = name.removePrefix(PREFIX).removeSuffix(FILE_TYPE)
}
