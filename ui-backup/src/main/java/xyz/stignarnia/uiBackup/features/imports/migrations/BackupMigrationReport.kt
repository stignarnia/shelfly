package xyz.stignarnia.uiBackup.features.imports.migrations

import xyz.stignarnia.uiBackup.features.imports.model.BackupUnmatchedItem
import xyz.stignarnia.uiBackup.features.imports.model.BackupUnmatchedList
import xyz.stignarnia.uiBackup.features.imports.model.BackupUnmatchedShow
import xyz.stignarnia.uiBackup.model.BackupScheme
import java.io.Serializable

/**
 * What a migration produced: the backup in the current scheme, plus what had to be dropped on the way there.
 */
data class BackupMigrationResult(
  val scheme: BackupScheme,
  val report: BackupMigrationReport = BackupMigrationReport(),
)

/**
 * Entries an older backup carried that could not be re-keyed onto TMDB ids.
 * These are losses, not errors: the import still runs, and the counts are shown once it finishes so the numbers are never silently wrong.
 */
data class BackupMigrationReport(
  val unmatchedShows: List<BackupUnmatchedShow> = emptyList(),
  val unmatchedMovies: List<BackupUnmatchedItem> = emptyList(),
  val unmatchedLists: List<BackupUnmatchedList> = emptyList(),
  val skippedSeasons: Int = 0,
  val skippedEpisodes: Int = 0,
  val skippedShowRatings: Int = 0,
  val skippedSeasonRatings: Int = 0,
  val skippedEpisodeRatings: Int = 0,
  val skippedMovieRatings: Int = 0,
) : Serializable {
  val isEmpty: Boolean
    get() =
      unmatchedShows.isEmpty() &&
        unmatchedMovies.isEmpty() &&
        unmatchedLists.isEmpty() &&
        skippedSeasons == 0 &&
        skippedEpisodes == 0 &&
        skippedShowRatings == 0 &&
        skippedSeasonRatings == 0 &&
        skippedEpisodeRatings == 0 &&
        skippedMovieRatings == 0
}
