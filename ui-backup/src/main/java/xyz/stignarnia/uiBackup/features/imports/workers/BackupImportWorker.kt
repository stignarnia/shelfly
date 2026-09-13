package xyz.stignarnia.uiBackup.features.imports.workers

import xyz.stignarnia.uiBackup.features.imports.model.BackupImportStatus
import xyz.stignarnia.uiBackup.features.imports.model.BackupUnmatchedItem
import xyz.stignarnia.uiBackup.features.imports.model.BackupUnmatchedShow
import xyz.stignarnia.uiBackup.model.BackupScheme

data class BackupImportWorkerResult(
  val importedShowsCount: Int,
  val importedMoviesCount: Int,
  val failedShows: List<BackupUnmatchedShow> = emptyList(),
  val failedMovies: List<BackupUnmatchedItem> = emptyList(),
)

interface BackupImportWorker {
  suspend fun run(backup: BackupScheme): BackupImportWorkerResult

  var statusListener: ((BackupImportStatus) -> Unit)?
}
