package com.michaldrabik.ui_backup.features.import_.migrations

import com.michaldrabik.ui_backup.model.BackupScheme

/**
 * What a migration produced: the backup in the current scheme, plus what had to
 * be dropped on the way there.
 */
data class BackupMigrationResult(
  val scheme: BackupScheme,
  val report: BackupMigrationReport = BackupMigrationReport(),
)

/**
 * Entries an older backup carried that could not be re-keyed onto TMDB ids.
 *
 * These are losses, not errors: the import still runs, and the counts are shown
 * once it finishes so the numbers are never silently wrong.
 */
data class BackupMigrationReport(
  val unmatchedShows: List<String> = emptyList(),
  val unmatchedMovies: List<String> = emptyList(),
  val skippedSeasons: Int = 0,
  val skippedEpisodes: Int = 0,
  val skippedShowRatings: Int = 0,
  val skippedSeasonRatings: Int = 0,
  val skippedEpisodeRatings: Int = 0,
  val skippedMovieRatings: Int = 0,
  val skippedListItems: Int = 0,
) {

  val isEmpty: Boolean
    get() = unmatchedShows.isEmpty() &&
      unmatchedMovies.isEmpty() &&
      skippedSeasons == 0 &&
      skippedEpisodes == 0 &&
      skippedShowRatings == 0 &&
      skippedSeasonRatings == 0 &&
      skippedEpisodeRatings == 0 &&
      skippedMovieRatings == 0 &&
      skippedListItems == 0
}
