@file:Suppress("DEPRECATION")

package xyz.stignarnia.data_local.database.migrations

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import xyz.stignarnia.data_local.database.AppDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the migrations against the schemas exported to `data-local/schemas`.
 *
 * These matter more than they look.
 * The database is built with `fallbackToDestructiveMigration(dropAllTables = true)`, so a migration that throws does not crash - Room drops every table and the user silently loses their whole library.
 * A failure here is the only warning that would ever be given.
 *
 * `runMigrationsAndValidate` compares the migrated database against the exported schema, so a migration that runs but produces the wrong shape fails too.
 */
@RunWith(AndroidJUnit4::class)
class MigrationsTest {

  companion object {
    private const val TEST_DB = "migration-test"
  }

  @get:Rule
  val helper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    AppDatabase::class.java,
  )

  private fun SupportSQLiteDatabase.insertShow(id: Long) = execSQL("INSERT INTO shows (id_tmdb) VALUES ($id)")

  private fun SupportSQLiteDatabase.insertSeason(
    id: Long,
    showId: Long,
    number: Int,
  ) = execSQL(
    "INSERT INTO seasons (id_tmdb, id_show_tmdb, season_number, season_title, season_overview, " +
      "episodes_count, episodes_aired_count, is_watched) " +
      "VALUES ($id, $showId, $number, '', '', 0, 0, 0)",
  )

  private fun SupportSQLiteDatabase.insertEpisode(
    id: Long,
    seasonId: Long,
    showId: Long,
    seasonNumber: Int,
    episodeNumber: Int,
  ) = execSQL(
    "INSERT INTO episodes (id_tmdb, id_season, id_show_tmdb, id_show_tvdb, id_show_imdb, season_number, " +
      "episode_number, episode_overview, episode_title, comments_count, rating, runtime, votes_count, is_watched) " +
      "VALUES ($id, $seasonId, $showId, -1, '', $seasonNumber, $episodeNumber, '', '', 0, 0, 0, 0, 0)",
  )

  private fun SupportSQLiteDatabase.insertRating(
    idTmdb: Long,
    type: String,
    rating: Int,
    seasonNumber: Int? = null,
    episodeNumber: Int? = null,
  ) = execSQL(
    "INSERT INTO ratings (id_tmdb, type, rating, season_number, episode_number, rated_at, created_at, updated_at) " +
      "VALUES ($idTmdb, '$type', $rating, ${seasonNumber ?: "NULL"}, ${episodeNumber ?: "NULL"}, 0, 0, 0)",
  )

  /**
   * Reads the ratings table as a list of rows, so assertions can talk about the whole table rather than one lookup.
   */
  private fun SupportSQLiteDatabase.readRatings(): List<String> {
    val rows = mutableListOf<String>()
    query(
      "SELECT id_tmdb, type, rating, season_number, episode_number FROM ratings ORDER BY id_tmdb, type, season_number, episode_number",
    ).use { cursor ->
      while (cursor.moveToNext()) {
        rows +=
          "${cursor.getLong(0)}|${cursor.getString(1)}|${cursor.getInt(2)}|${cursor.getInt(3)}|${cursor.getInt(4)}"
      }
    }
    return rows
  }

  @Test
  fun migration43To44ReKeysASeasonRatingTheAppWroteOntoItsShow() {
    helper.createDatabase(TEST_DB, 43).use { db ->
      db.insertShow(100)
      db.insertSeason(id = 500, showId = 100, number = 2)
      // The app keyed the rating by the season's own id and left the numbers null.
      db.insertRating(idTmdb = 500, type = "season", rating = 8)
    }

    val db = helper.runMigrationsAndValidate(TEST_DB, 44, true, MIGRATION_43_44)

    assertThat(db.readRatings()).containsExactly("100|season|8|2|-1")
  }

  @Test
  fun migration43To44ReKeysAnEpisodeRatingTheAppWroteOntoItsShow() {
    helper.createDatabase(TEST_DB, 43).use { db ->
      db.insertShow(100)
      db.insertSeason(id = 500, showId = 100, number = 2)
      db.insertEpisode(id = 900, seasonId = 500, showId = 100, seasonNumber = 2, episodeNumber = 7)
      db.insertRating(idTmdb = 900, type = "episode", rating = 9)
    }

    val db = helper.runMigrationsAndValidate(TEST_DB, 44, true, MIGRATION_43_44)

    assertThat(db.readRatings()).containsExactly("100|episode|9|2|7")
  }

  @Test
  fun migration43To44KeepsAnImportedShowKeyedRating() {
    helper.createDatabase(TEST_DB, 43).use { db ->
      db.insertShow(200)
      // An import wrote the show id and the season number, which is the identity a backup file records.
      db.insertRating(idTmdb = 200, type = "season", rating = 7, seasonNumber = 3)
    }

    val db = helper.runMigrationsAndValidate(TEST_DB, 44, true, MIGRATION_43_44)

    assertThat(db.readRatings()).containsExactly("200|season|7|3|-1")
  }

  @Test
  fun migration43To44KeepsShowAndMovieRatingsUnchanged() {
    helper.createDatabase(TEST_DB, 43).use { db ->
      db.insertRating(idTmdb = 300, type = "show", rating = 6)
      db.insertRating(idTmdb = 400, type = "movie", rating = 5)
    }

    val db = helper.runMigrationsAndValidate(TEST_DB, 44, true, MIGRATION_43_44)

    assertThat(db.readRatings()).containsExactly("300|show|6|-1|-1", "400|movie|5|-1|-1")
  }

  @Test
  fun migration43To44DropsARatingThatResolvesToNothing() {
    helper.createDatabase(TEST_DB, 43).use { db ->
      // Neither a known season nor a known show, so there is nothing left to identify it by.
      db.insertRating(idTmdb = 999, type = "season", rating = 4)
    }

    val db = helper.runMigrationsAndValidate(TEST_DB, 44, true, MIGRATION_43_44)

    assertThat(db.readRatings()).isEmpty()
  }

  @Test
  fun migration43To44LetsAnAppWrittenRatingWinOverAnImportedOneForTheSameSeason() {
    helper.createDatabase(TEST_DB, 43).use { db ->
      db.insertShow(100)
      db.insertSeason(id = 500, showId = 100, number = 2)
      // Both rows describe season 2 of show 100 once re-keyed, and the app's row is meant to win.
      db.insertRating(idTmdb = 100, type = "season", rating = 3, seasonNumber = 2)
      db.insertRating(idTmdb = 500, type = "season", rating = 8)
    }

    val db = helper.runMigrationsAndValidate(TEST_DB, 44, true, MIGRATION_43_44)

    assertThat(db.readRatings()).containsExactly("100|season|8|2|-1")
  }

  @Test
  fun migration44To45AddsNetworkLogoPathDefaultingToEmpty() {
    helper.createDatabase(TEST_DB, 44).use { db ->
      db.insertShow(100)
    }

    val db = helper.runMigrationsAndValidate(TEST_DB, 45, true, MIGRATION_44_45)

    db.query("SELECT network_logo_path FROM shows WHERE id_tmdb = 100").use { cursor ->
      assertThat(cursor.moveToFirst()).isTrue()
      assertThat(cursor.getString(0)).isEmpty()
    }
  }

  @Test
  fun migrationsRunAsAChainFrom43To45() {
    helper.createDatabase(TEST_DB, 43).use { db ->
      db.insertShow(100)
      db.insertSeason(id = 500, showId = 100, number = 2)
      db.insertRating(idTmdb = 500, type = "season", rating = 8)
    }

    val db = helper.runMigrationsAndValidate(TEST_DB, 45, true, MIGRATION_43_44, MIGRATION_44_45)

    assertThat(db.readRatings()).containsExactly("100|season|8|2|-1")
  }
}
