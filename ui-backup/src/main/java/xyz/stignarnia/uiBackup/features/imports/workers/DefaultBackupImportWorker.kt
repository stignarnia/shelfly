package xyz.stignarnia.uiBackup.features.imports.workers

import kotlinx.coroutines.withContext
import xyz.stignarnia.common.dispatchers.CoroutineDispatchers
import xyz.stignarnia.uiBackup.features.imports.model.BackupImportStatus
import xyz.stignarnia.uiBackup.features.imports.runners.BackupImportListsRunner
import xyz.stignarnia.uiBackup.features.imports.runners.BackupImportMoviesRunner
import xyz.stignarnia.uiBackup.features.imports.runners.BackupImportShowsRunner
import xyz.stignarnia.uiBackup.model.BackupScheme
import javax.inject.Inject

internal class DefaultBackupImportWorker
  @Inject
  constructor(
    private val dispatchers: CoroutineDispatchers,
    private val importShowsRunner: BackupImportShowsRunner,
    private val importMoviesRunner: BackupImportMoviesRunner,
    private val importListsRunner: BackupImportListsRunner,
  ) : BackupImportWorker {
    override var statusListener: ((BackupImportStatus) -> Unit)? = null
      set(value) {
        field = value
        importShowsRunner.statusListener = value
        importMoviesRunner.statusListener = value
        importListsRunner.statusListener = value
      }

    override suspend fun run(backup: BackupScheme): BackupImportWorkerResult =
      withContext(dispatchers.IO) {
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

        val uniqueShows =
          (backup.shows.collectionHistory + backup.shows.collectionWatchlist + backup.shows.collectionHidden)
            .distinctBy { it.tmdbId }
        val uniqueMovies =
          (backup.movies.collectionHistory + backup.movies.collectionWatchlist + backup.movies.collectionHidden)
            .distinctBy { it.tmdbId }

        val failedShows = importShowsRunner.failedShows.toList()
        val failedMovies = importMoviesRunner.failedMovies.toList()

        val failedEntireShowsCount = failedShows.count { it.isEntireShowUnmatched }
        val importedShowsCount = (uniqueShows.size - failedEntireShowsCount).coerceAtLeast(0)
        val importedMoviesCount = (uniqueMovies.size - failedMovies.size).coerceAtLeast(0)

        BackupImportWorkerResult(
          importedShowsCount = importedShowsCount,
          importedMoviesCount = importedMoviesCount,
          failedShows = failedShows,
          failedMovies = failedMovies,
        )
      }
  }
