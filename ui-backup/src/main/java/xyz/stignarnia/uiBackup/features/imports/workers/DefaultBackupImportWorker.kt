package xyz.stignarnia.uiBackup.features.imports.workers

import kotlinx.coroutines.coroutineScope
import xyz.stignarnia.uiBackup.features.imports.model.BackupImportStatus
import xyz.stignarnia.uiBackup.features.imports.runners.BackupImportListsRunner
import xyz.stignarnia.uiBackup.features.imports.runners.BackupImportMoviesRunner
import xyz.stignarnia.uiBackup.features.imports.runners.BackupImportShowsRunner
import xyz.stignarnia.uiBackup.model.BackupScheme
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultBackupImportWorker
  @Inject
  constructor(
    private val importShowsRunner: BackupImportShowsRunner,
    private val importMoviesRunner: BackupImportMoviesRunner,
    private val importListsRunner: BackupImportListsRunner,
  ) : BackupImportWorker {
    override var statusListener: ((BackupImportStatus) -> Unit)? = null
      set(value) {
        field = value
        importShowsRunner.statusListener = field
        importMoviesRunner.statusListener = field
        importListsRunner.statusListener = field
      }

    override suspend fun run(backup: BackupScheme) {
      coroutineScope {
        importShowsRunner.run(backup.shows)
        importMoviesRunner.run(backup.movies)
        importListsRunner.run(backup.lists)
      }
    }
  }
