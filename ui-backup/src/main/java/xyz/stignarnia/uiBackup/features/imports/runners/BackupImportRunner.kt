package xyz.stignarnia.uiBackup.features.imports.runners

import xyz.stignarnia.uiBackup.features.imports.model.BackupImportStatus

internal abstract class BackupImportRunner<T> {
  var statusListener: ((BackupImportStatus) -> Unit)? = null

  abstract suspend fun run(backup: T)
}
