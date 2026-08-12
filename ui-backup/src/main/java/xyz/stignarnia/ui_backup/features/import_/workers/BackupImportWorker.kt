package xyz.stignarnia.ui_backup.features.import_.workers

import xyz.stignarnia.ui_backup.features.import_.model.BackupImportStatus
import xyz.stignarnia.ui_backup.model.BackupScheme

interface BackupImportWorker {
  suspend fun run(backup: BackupScheme)

  var statusListener: ((BackupImportStatus) -> Unit)?
}
