package xyz.stignarnia.uiBackup.features.imports.model

sealed interface BackupImportStatus {
  data object Idle : BackupImportStatus

  data object Initializing : BackupImportStatus

  data class Importing(
    val title: String,
  ) : BackupImportStatus
}
