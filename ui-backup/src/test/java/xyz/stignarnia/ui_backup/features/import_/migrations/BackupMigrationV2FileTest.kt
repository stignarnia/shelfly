package xyz.stignarnia.ui_backup.features.import_.migrations

import com.google.common.truth.Truth.assertThat
import xyz.stignarnia.common_test.UnconfinedCoroutineDispatchers
import xyz.stignarnia.ui_backup.features.import_.migrations.model.BackupMoviesV2
import xyz.stignarnia.ui_backup.features.import_.migrations.model.BackupSchemeV2
import xyz.stignarnia.ui_backup.features.import_.migrations.model.BackupShowsV2
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Runs the migration over a real v2 export.
 *
 * Self-skips unless `SHELFLY_V2_BACKUP` points at one, so no personal watch
 * history ever has to live in the repository and CI stays green. Run it with:
 *
 *   SHELFLY_V2_BACKUP=/path/to/showly_export.json ./gradlew :ui-backup:testDebugUnitTest
 *
 * The resolver is stubbed out, so this measures the offline remap only - what
 * the file can recover on its own, without a TMDB round trip.
 */
class BackupMigrationV2FileTest {

  private val SUT = BackupMigrationV2(
    dispatchers = UnconfinedCoroutineDispatchers(),
    resolver = object : CatalogIdResolver {
      override suspend fun findShowByTitle(title: String): Long? = null

      override suspend fun findMovieByTitle(title: String): Long? = null
    },
  )

  private fun exportFile(): File? =
    System
      .getenv("SHELFLY_V2_BACKUP")
      ?.takeIf { it.isNotBlank() }
      ?.let(::File)
      ?.takeIf { it.exists() }

  @Test
  fun `Should account for every entry in a real export`() =
    runTest {
      val file = exportFile()
      assumeTrue("SHELFLY_V2_BACKUP is not set. Skipping.", file != null)

      val source = Moshi
        .Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter(BackupSchemeV2::class.java)
        .fromJson(file!!.readText())!!

      assertThat(source.version).isEqualTo(BackupMigrationV2.VERSION)

      val (scheme, report) = SUT.migrate(source)

      // Nothing may vanish unreported: kept + skipped has to equal the input.
      assertThat(scheme.shows.progressEpisodes.size + report.skippedEpisodes)
        .isEqualTo(source.shows.progressEpisodes.size)
      assertThat(scheme.shows.progressSeasons.size + report.skippedSeasons)
        .isEqualTo(source.shows.progressSeasons.size)
      assertThat(scheme.shows.ratingsShows.size + report.skippedShowRatings)
        .isEqualTo(source.shows.ratingsShows.size)
      assertThat(scheme.shows.ratingsSeasons.size + report.skippedSeasonRatings)
        .isEqualTo(source.shows.ratingsSeasons.size)
      assertThat(scheme.shows.ratingsEpisodes.size + report.skippedEpisodeRatings)
        .isEqualTo(source.shows.ratingsEpisodes.size)
      assertThat(scheme.movies.ratingsMovies.size + report.skippedMovieRatings)
        .isEqualTo(source.movies.ratingsMovies.size)

      val collectedShows = with(scheme.shows) {
        (collectionHistory + collectionWatchlist + collectionHidden).map { it.tmdbId }.toSet()
      }
      val collectedMovies = with(scheme.movies) {
        (collectionHistory + collectionWatchlist + collectionHidden).map { it.tmdbId }.toSet()
      }

      // Every id that survived is usable, and every watched episode still hangs
      // off a show that was actually imported.
      assertThat(collectedShows.none { it <= 0 }).isTrue()
      assertThat(collectedMovies.none { it <= 0 }).isTrue()
      assertThat(collectedShows).containsAtLeastElementsIn(
        scheme.shows.progressEpisodes
          .map { it.showTmdbId }
          .toSet(),
      )

      println(
        """
        |Migrated ${file.name}:
        |  shows      ${collectedShows.size} / ${source.shows.collectionSize()} (unmatched: ${report.unmatchedShows})
        |  movies     ${collectedMovies.size} / ${source.movies.collectionSize()} (unmatched: ${report.unmatchedMovies})
        |  episodes   ${scheme.shows.progressEpisodes.size} / ${source.shows.progressEpisodes.size}
        |  seasons    ${scheme.shows.progressSeasons.size} / ${source.shows.progressSeasons.size}
        |  s.ratings  ${scheme.shows.ratingsSeasons.size} / ${source.shows.ratingsSeasons.size}
        |  lists      ${scheme.lists.lists.size}, items skipped: ${report.skippedListItems}
        """.trimMargin(),
      )
    }
}

private fun BackupShowsV2.collectionSize() =
  (collectionHistory + collectionWatchlist + collectionHidden).map { it.legacyId }.distinct().size

private fun BackupMoviesV2.collectionSize() =
  (collectionHistory + collectionWatchlist + collectionHidden).map { it.legacyId }.distinct().size
