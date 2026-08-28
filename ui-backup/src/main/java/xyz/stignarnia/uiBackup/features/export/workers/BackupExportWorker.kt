package xyz.stignarnia.uiBackup.features.export.workers

import xyz.stignarnia.uiBackup.model.BackupScheme

interface BackupExportWorker {
  suspend fun run(): BackupScheme
}
