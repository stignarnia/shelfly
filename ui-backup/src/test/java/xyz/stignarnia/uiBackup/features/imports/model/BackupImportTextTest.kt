package xyz.stignarnia.uiBackup.features.imports.model

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import org.junit.Test

class BackupImportTextTest {
  private val adapter =
    Moshi
      .Builder()
      .add(BackupImportText.LegacyAdapter)
      .build()
      .adapter(BackupImportResult::class.java)

  @Test
  fun `Should read a report saved while titles and reasons were plain strings`() {
    val json =
      """
      {
        "importedMoviesCount": 1,
        "importedShowsCount": 0,
        "unmatchedMovies": [{ "title": "Heat", "reason": "No results found on TMDB." }],
        "unmatchedShows": [{ "title": "Lost", "unmatchedSeasons": [{ "seasonNumber": 2, "reason": "Stagione non trovata su TMDB." }] }],
        "unmatchedLists": [{ "title": "Favourites", "reason": null }]
      }
      """.trimIndent()

    val result = adapter.fromJson(json)!!

    assertThat(result.unmatchedMovies.single().title).isEqualTo(BackupImportText.verbatim("Heat"))
    assertThat(result.unmatchedMovies.single().reason).isEqualTo(BackupImportText.verbatim("No results found on TMDB."))
    assertThat(
      result.unmatchedShows
        .single()
        .unmatchedSeasons
        .single()
        .reason,
    ).isEqualTo(BackupImportText.verbatim("Stagione non trovata su TMDB."))
    assertThat(result.unmatchedShows.single().reason).isNull()
    assertThat(result.unmatchedLists.single().reason).isNull()
  }

  @Test
  fun `Should keep a message and its arguments across saving and loading`() {
    val result =
      BackupImportResult(
        importedMoviesCount = 0,
        importedShowsCount = 0,
        unmatchedMovies =
          listOf(
            BackupUnmatchedItem(
              title = BackupImportText.of(BackupImportText.Message.MOVIE_TMDB_ID, 603L),
              reason = BackupImportText.of(BackupImportText.Message.TMDB_API_ERROR, 500),
            ),
          ),
        unmatchedLists = listOf(BackupUnmatchedList(title = "Favourites", reason = BackupImportText.of(BackupImportText.Message.LIST_MERGED))),
      )

    assertThat(adapter.fromJson(adapter.toJson(result))).isEqualTo(result)
  }
}
