package xyz.stignarnia.uiBackup.features.imports.model

sealed interface BackupImportStatus {
  data object Idle : BackupImportStatus

  data object Initializing : BackupImportStatus

  data class Importing(
    val title: String = "",
    val current: Int = 0,
    val total: Int = 0,
  ) : BackupImportStatus
}
