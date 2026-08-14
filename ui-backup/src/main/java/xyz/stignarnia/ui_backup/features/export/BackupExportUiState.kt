package xyz.stignarnia.ui_backup.features.export

import android.net.Uri
import xyz.stignarnia.ui_backup.features.export.model.BackupExportSchedule
import java.time.format.DateTimeFormatter

data class BackupExportUiState(
  val isLoading: Boolean = false,
  val exportContent: ExportContentState? = null,
  val error: Throwable? = null,
  val backupExportSchedule: BackupExportSchedule = BackupExportSchedule.DEFAULT_OFF,
  val lastBackupExportTimestamp: Long = 0L,
  val syncStatus: SyncStatus? = null,
  val dateFormat: DateTimeFormatter? = null,
)

/**
 * What this device's syncing looks like right now.
 *
 * A failure is reported alongside the last success rather than instead of it,
 * so a device that has been failing for a week cannot pass for one that simply
 * synced a while ago.
 */
data class SyncStatus(
  val deviceId: String,
  val lastSyncedAt: Long,
  val peers: Set<String>,
  val error: String?,
)

data class ExportContentState(
  val exportContent: String,
  val exportUri: Uri,
)
