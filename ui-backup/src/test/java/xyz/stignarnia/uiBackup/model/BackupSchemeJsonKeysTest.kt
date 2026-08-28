package xyz.stignarnia.uiBackup.model

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import org.junit.Test

/**
 * Locks the JSON keys an export is written with.
 *
 * Every key here is abbreviated through @param:Json, and the abbreviation is the wire format: a file written with the property names instead would be unreadable by every Shelfly already installed, and unreadable to every other device syncing over WebDAV.
 * Nothing else fails when the mapping is lost - the models still compile, the round trip inside one build still passes, and only a user restoring an older export ever finds out.
 *
 * That is not hypothetical.
 * Moshi reads @Json off the constructor parameter, so the use-site target these models carry is what makes the mapping visible to the code generator at all.
 */
class BackupSchemeJsonKeysTest {
  private val adapter =
    Moshi
      .Builder()
      .build()
      .adapter(BackupScheme::class.java)

  private fun keysOf(json: String) = Regex("\"([A-Za-z]+)\":").findAll(json).map { it.groupValues[1] }.toSet()

  @Test
  fun `Should write shows under the abbreviated keys`() {
    val json =
      adapter.toJson(
        BackupScheme(
          version = 3,
          platform = "android",
          createdAt = "2026-08-25T00:00:00.000Z",
          shows =
            BackupShows(
              collectionHistory = listOf(BackupShow(tmdbId = 1, title = "T", addedAt = "a", updatedAt = "u")),
              progressEpisodes = listOf(BackupEpisode(showTmdbId = 1, episodeNumber = 2, seasonNumber = 3, addedAt = "a")),
              progressPinned = listOf(1L),
              ratingsShows = listOf(BackupShowRating(tmdbId = 1, rating = 5, ratedAt = "r")),
            ),
        ),
      )

    assertThat(keysOf(json)).containsAtLeast("cH", "pEp", "pP", "rS")
    assertThat(keysOf(json)).containsAtLeast("tmId", "t", "a", "u", "eN", "sN", "r", "rA")
    assertThat(keysOf(json)).containsNoneOf("collectionHistory", "progressEpisodes", "tmdbId", "addedAt")
  }

  @Test
  fun `Should write movies and lists under the abbreviated keys`() {
    val json =
      adapter.toJson(
        BackupScheme(
          version = 3,
          platform = "android",
          createdAt = "2026-08-25T00:00:00.000Z",
          movies =
            BackupMovies(
              collectionHistory = listOf(BackupMovie(tmdbId = 1, title = "T", addedAt = "a")),
              ratingsMovies = listOf(BackupMovieRating(tmdbId = 1, rating = 5, ratedAt = "r")),
            ),
          lists =
            BackupLists(
              lists =
                listOf(
                  BackupList(
                    id = 1,
                    slugId = "s",
                    name = "n",
                    description = null,
                    privacy = "p",
                    itemCount = 1,
                    createdAt = "c",
                    updatedAt = "u",
                    items =
                      listOf(
                        BackupListItem(
                          id = 1,
                          listId = 1,
                          tmdbId = 1,
                          type = "show",
                          rank = 1,
                          listedAt = "l",
                          createdAt = "c",
                          updatedAt = "u",
                        ),
                      ),
                  ),
                ),
            ),
        ),
      )

    assertThat(keysOf(json)).containsAtLeast("cH", "rM", "l", "sId", "n", "p", "ic", "it", "lId", "tmId")
    assertThat(keysOf(json)).containsNoneOf("collectionHistory", "ratingsMovies", "slugId", "itemCount", "listId")
  }

  @Test
  fun `Should read back a file written with the abbreviated keys`() {
    val json =
      """{"version":3,"platform":"android","createdAt":"c","shows":{"cH":[{"tmId":7,"t":"Show","a":"a","u":"u"}]}}"""

    val scheme = adapter.fromJson(json)!!
    val show = scheme.shows.collectionHistory.single()

    assertThat(show.tmdbId).isEqualTo(7L)
    assertThat(show.title).isEqualTo("Show")
    // The absent collections come from the constructor defaults, which only a Kotlin-aware adapter applies.
    assertThat(scheme.movies.collectionHistory).isEmpty()
    assertThat(scheme.lists.lists).isEmpty()
  }
}
