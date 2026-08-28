package xyz.stignarnia.uiBackup.features.imports.migrations

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import xyz.stignarnia.commonTest.UnconfinedCoroutineDispatchers
import xyz.stignarnia.uiBackup.BackupConfig.SCHEME_VERSION

class BackupMigrationV2Test {
  private val resolver =
    FakeCatalogIdResolver(
      shows = mapOf("Findable Show" to 2000L),
      movies = mapOf("Findable Movie" to 901L),
    )

  private val SUT =
    BackupMigrationV2(
      dispatchers = UnconfinedCoroutineDispatchers(),
      resolver = resolver,
    )

  private fun fixture() = javaClass.getResource("/backup_v2.json")!!.readText()

  @Test
  fun `Should migrate onto the current scheme version`() =
    runTest {
      val result = SUT.migrate(fixture())

      assertThat(result.scheme.version).isEqualTo(SCHEME_VERSION)
      assertThat(result.scheme.platform).isEqualTo("android")
      assertThat(result.scheme.createdAt).isEqualTo("2026-08-11T14:36:24.709Z")
    }

  @Test
  fun `Should key shows by their TMDB id, looking up the ones that have none`() =
    runTest {
      val shows = SUT.migrate(fixture()).scheme.shows

      assertThat(shows.collectionHistory.map { it.tmdbId }).containsExactly(1000L, 2000L)
      assertThat(shows.collectionWatchlist.map { it.tmdbId }).containsExactly(4000L)
      assertThat(shows.collectionHidden.map { it.tmdbId }).containsExactly(5000L)

      val kept = shows.collectionHistory.first { it.tmdbId == 1000L }
      assertThat(kept.title).isEqualTo("Kept Show")
      assertThat(kept.addedAt).isEqualTo("2024-01-01T00:00:00Z")
      assertThat(kept.updatedAt).isEqualTo("2024-02-01T00:00:00Z")
    }

  @Test
  fun `Should report shows that cannot be matched at all`() =
    runTest {
      val report = SUT.migrate(fixture()).report

      assertThat(report.isEmpty).isFalse()
      assertThat(report.unmatchedShows).containsExactly("Gone Show")
      assertThat(report.unmatchedMovies).isEmpty()
    }

  @Test
  fun `Should take the episode's parent from its show id, never from the exported TMDB id`() =
    runTest {
      // "stmId" on a v2 episode points at something other than the parent show, so an episode of "Kept Show" has to come back keyed 1000, not 99999.
      val result = SUT.migrate(fixture())
      val episodes = result.scheme.shows.progressEpisodes

      assertThat(episodes.map { it.showTmdbId }).containsExactly(1000L, 2000L)
      assertThat(episodes.map { it.showTmdbId }).doesNotContain(99999L)

      val watched = episodes.first { it.showTmdbId == 1000L }
      assertThat(watched.seasonNumber).isEqualTo(1)
      assertThat(watched.episodeNumber).isEqualTo(1)
      assertThat(watched.addedAt).isEqualTo("2024-03-01T00:00:00Z")

      assertThat(result.report.skippedEpisodes).isEqualTo(1)
    }

  @Test
  fun `Should fall back to the exported TMDB id for seasons, where it is trustworthy`() =
    runTest {
      val result = SUT.migrate(fixture())
      val seasons = result.scheme.shows.progressSeasons

      assertThat(seasons.map { it.showTmdbId to it.seasonNumber })
        .containsExactly(1000L to 1, 5000L to 2)
      assertThat(result.report.skippedSeasons).isEqualTo(1)
    }

  @Test
  fun `Should remap pinned and on hold ids, which v2 stored as legacy ids`() =
    runTest {
      val shows = SUT.migrate(fixture()).scheme.shows

      assertThat(shows.progressPinned).containsExactly(1000L)
      assertThat(shows.progressOnHold).containsExactly(4000L)
    }

  @Test
  fun `Should migrate ratings and count the unrecoverable ones`() =
    runTest {
      val result = SUT.migrate(fixture())
      val shows = result.scheme.shows

      assertThat(shows.ratingsShows.map { it.tmdbId }).containsExactly(1000L)
      assertThat(shows.ratingsSeasons.map { it.showTmdbId }).containsExactly(1000L)
      assertThat(shows.ratingsEpisodes.map { it.showTmdbId }).containsExactly(1000L)

      with(result.report) {
        assertThat(skippedShowRatings).isEqualTo(1)
        assertThat(skippedSeasonRatings).isEqualTo(1)
        assertThat(skippedEpisodeRatings).isEqualTo(1)
        assertThat(skippedMovieRatings).isEqualTo(0)
      }
    }

  @Test
  fun `Should migrate movies and their pinned ids`() =
    runTest {
      val movies = SUT.migrate(fixture()).scheme.movies

      assertThat(movies.collectionHistory.map { it.tmdbId }).containsExactly(900L)
      assertThat(movies.collectionWatchlist.map { it.tmdbId }).containsExactly(901L)
      assertThat(movies.collectionHidden).isEmpty()
      assertThat(movies.progressPinned).containsExactly(900L)
      assertThat(movies.ratingsMovies.map { it.tmdbId }).containsExactly(900L)
    }

  @Test
  fun `Should keep list items that resolve and count the ones that do not`() =
    runTest {
      val result = SUT.migrate(fixture())
      val list =
        result.scheme.lists.lists
          .single()

      assertThat(list.slugId).isEqualTo("my-list")
      assertThat(list.name).isEqualTo("My List")
      assertThat(list.items.map { it.tmdbId }).containsExactly(1000L)
      assertThat(result.report.skippedListItems).isEqualTo(1)
    }

  @Test
  fun `Should drop everything under a show it could not match`() =
    runTest {
      val result = SUT.migrate(fixture())
      val collected =
        with(result.scheme.shows) {
          (collectionHistory + collectionWatchlist + collectionHidden).map { it.tmdbId }
        }

      with(result.scheme.shows) {
        assertThat(collected).containsNoDuplicates()
        assertThat(collected).containsAtLeastElementsIn(progressEpisodes.map { it.showTmdbId })
        assertThat(collected).containsAtLeastElementsIn(progressSeasons.map { it.showTmdbId })
      }
    }

  @Test
  fun `Should report nothing when every entry carries a TMDB id`() =
    runTest {
      val json =
        """
        {"version":2,"platform":"android","createdAt":"2026-01-01T00:00:00Z",
         "shows":{"cH":[{"id":1,"tmId":11,"t":"A","a":"2024-01-01T00:00:00Z","u":"2024-01-01T00:00:00Z"}],
                  "pEp":[{"id":1,"sId":1,"stmId":-1,"eN":1,"sN":1,"a":null}]},
         "movies":{},"lists":{}}
        """.trimIndent()

      val result = SUT.migrate(json)

      assertThat(result.report.isEmpty).isTrue()
      assertThat(
        result.scheme.shows.progressEpisodes
          .single()
          .showTmdbId,
      ).isEqualTo(11L)
    }
}

private class FakeCatalogIdResolver(
  private val shows: Map<String, Long> = emptyMap(),
  private val movies: Map<String, Long> = emptyMap(),
) : CatalogIdResolver {
  override suspend fun findShowByTitle(title: String) = shows[title]

  override suspend fun findMovieByTitle(title: String) = movies[title]
}
