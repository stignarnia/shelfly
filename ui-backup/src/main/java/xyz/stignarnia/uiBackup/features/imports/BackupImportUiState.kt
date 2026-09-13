package xyz.stignarnia.uiBackup.features.imports

import xyz.stignarnia.uiBackup.features.imports.migrations.BackupMigrationReport
import xyz.stignarnia.uiBackup.features.imports.model.BackupImportResult
import xyz.stignarnia.uiBackup.features.imports.model.BackupImportStatus
import xyz.stignarnia.uiBackup.features.imports.model.BackupImportStatus.Idle
import xyz.stignarnia.uiBackup.features.imports.model.WebDavBackups

data class BackupImportUiState(
  val isImporting: BackupImportStatus = Idle,
  val isSuccess: Boolean = false,
  val isError: Throwable? = null,
  val report: BackupMigrationReport? = null,
  val result: BackupImportResult? = null,
  val webDavBackups: WebDavBackups = WebDavBackups.Idle,
  val hasLastReport: Boolean = false,
)
