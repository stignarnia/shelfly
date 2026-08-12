package xyz.stignarnia.ui_backup.features.export.workers

import xyz.stignarnia.ui_backup.model.BackupScheme

interface BackupExportWorker {
  suspend fun run(): BackupScheme
}
