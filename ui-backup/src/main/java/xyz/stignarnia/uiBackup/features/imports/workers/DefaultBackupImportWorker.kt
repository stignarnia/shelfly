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
        val showsTotal =
          backup.shows.collectionHistory.size +
            backup.shows.collectionWatchlist.size +
            backup.shows.collectionHidden.size

        val moviesTotal =
          backup.movies.collectionHistory.size +
            backup.movies.collectionWatchlist.size +
            backup.movies.collectionHidden.size

        val listItemsTotal = backup.lists.lists.sumOf { it.items.size }
        val listsTotal = if (listItemsTotal > 0) listItemsTotal else backup.lists.lists.size

        val grandTotal = showsTotal + moviesTotal + listsTotal

        val showsEnd = importShowsRunner.run(backup.shows, startCount = 0, total = grandTotal)
        val moviesEnd = importMoviesRunner.run(backup.movies, startCount = showsEnd, total = grandTotal)
        importListsRunner.run(backup.lists, startCount = moviesEnd, total = grandTotal)
      }
    }
  }
