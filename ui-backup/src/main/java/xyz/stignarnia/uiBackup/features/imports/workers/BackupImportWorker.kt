package xyz.stignarnia.uiBackup.features.imports.workers

import xyz.stignarnia.uiBackup.features.imports.model.BackupImportStatus
import xyz.stignarnia.uiBackup.model.BackupScheme

interface BackupImportWorker {
  suspend fun run(backup: BackupScheme)

  var statusListener: ((BackupImportStatus) -> Unit)?
}
