package xyz.stignarnia.ui_backup.features.export.workers

import xyz.stignarnia.common.extensions.dateIsoStringFromMillis
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.ui_backup.BackupConfig.SCHEME_PLATFORM
import xyz.stignarnia.ui_backup.BackupConfig.SCHEME_VERSION
import xyz.stignarnia.ui_backup.features.export.runners.BackupExportListsRunner
import xyz.stignarnia.ui_backup.features.export.runners.BackupExportMoviesRunner
import xyz.stignarnia.ui_backup.features.export.runners.BackupExportShowsRunner
import xyz.stignarnia.ui_backup.model.BackupScheme
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultBackupExportWorker @Inject constructor(
  private val exportShowsRunner: BackupExportShowsRunner,
  private val exportMoviesRunner: BackupExportMoviesRunner,
  private val exportListsRunner: BackupExportListsRunner,
) : BackupExportWorker {

  override suspend fun run(): BackupScheme {
    val exportShows = exportShowsRunner.run()
    val exportMovies = exportMoviesRunner.run()
    val exportLists = exportListsRunner.run()

    return BackupScheme(
      version = SCHEME_VERSION,
      platform = SCHEME_PLATFORM,
      createdAt = dateIsoStringFromMillis(nowUtcMillis()),
      shows = exportShows,
      movies = exportMovies,
      lists = exportLists,
    )
  }
}
