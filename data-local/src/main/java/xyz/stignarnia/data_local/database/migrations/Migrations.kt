package xyz.stignarnia.data_local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

const val DATABASE_VERSION = 45
const val DATABASE_NAME = "SHELFLY_DB"

/**
 * Adds the tombstone table backing multi-device sync.
 *
 * Purely additive, so unlike the jump to 42 this is a real migration rather than a wipe: there is user data to preserve now.
 */
val MIGRATION_42_43 = object : Migration(42, 43) {
  override fun migrate(connection: SQLiteConnection) {
    connection.execSQL(
      "CREATE TABLE IF NOT EXISTS `sync_tombstones` (`entity_type` TEXT NOT NULL, `entity_key` TEXT NOT NULL, `deleted_at` INTEGER NOT NULL, PRIMARY KEY(`entity_type`, `entity_key`))",
    )
  }
}

/**
 * Re-keys season and episode ratings onto the show they belong to.
 *
 * Until now the key was `(id_tmdb, type)`, where `id_tmdb` held the season's or episode's own TMDB id.
 * Nothing outside the database could speak that identity: a backup file records a child by its parent show plus season and episode number, and so do the sync keys.
 * Two things followed from that.
 *
 * A rating written by the app was invisible after an export/import round trip, because the importer had no season id to write and used the show id instead.
 * And because the old key ignored the numbers, every season rating of one show collided on import - a real export of 1962 season ratings collapsed to 90 rows, one per show, each holding whichever season came last.
 *
 * The new key is `(id_tmdb, type, season_number, episode_number)` with `id_tmdb` always the show (or movie).
 * Existing rows are sorted into three cases:
 *
 * - written by the app, so `id_tmdb` resolves in `seasons` / `episodes`: the parent show is read off that row and becomes the new `id_tmdb`;
 * - written by an import, so `id_tmdb` is already a show id: kept as is, which is the first time those rows become readable;
 * - neither: dropped, because there is nothing left to identify them by.
 *
 * The second pass runs `INSERT OR REPLACE` so a rating the app wrote always wins over an imported row for the same season.
 */
val MIGRATION_43_44 = object : Migration(43, 44) {
  override fun migrate(connection: SQLiteConnection) {
    connection.execSQL(
      "CREATE TABLE IF NOT EXISTS `ratings_new` (`id_tmdb` INTEGER NOT NULL, `type` TEXT NOT NULL, " +
        "`rating` INTEGER NOT NULL, `season_number` INTEGER NOT NULL, `episode_number` INTEGER NOT NULL, " +
        "`rated_at` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, " +
        "PRIMARY KEY(`id_tmdb`, `type`, `season_number`, `episode_number`))",
    )

    // Shows and movies were always keyed by their own id and carry no numbers.
    connection.execSQL(
      "INSERT OR IGNORE INTO `ratings_new` " +
        "SELECT id_tmdb, type, rating, -1, -1, rated_at, created_at, updated_at " +
        "FROM `ratings` WHERE type IN ('show', 'movie')",
    )

    // Rows an import wrote: already show-keyed, so only the numbers need a value.
    connection.execSQL(
      "INSERT OR IGNORE INTO `ratings_new` " +
        "SELECT r.id_tmdb, r.type, r.rating, COALESCE(r.season_number, -1), " +
        "CASE WHEN r.type = 'episode' THEN COALESCE(r.episode_number, -1) ELSE -1 END, " +
        "r.rated_at, r.created_at, r.updated_at " +
        "FROM `ratings` r " +
        "WHERE r.type IN ('season', 'episode') " +
        "AND r.season_number IS NOT NULL " +
        "AND EXISTS (SELECT 1 FROM `shows` s WHERE s.id_tmdb = r.id_tmdb)",
    )

    // Rows the app wrote: the season carries the show it belongs to.
    connection.execSQL(
      "INSERT OR REPLACE INTO `ratings_new` " +
        "SELECT s.id_show_tmdb, r.type, r.rating, COALESCE(r.season_number, s.season_number), -1, " +
        "r.rated_at, r.created_at, r.updated_at " +
        "FROM `ratings` r JOIN `seasons` s ON s.id_tmdb = r.id_tmdb " +
        "WHERE r.type = 'season'",
    )

    connection.execSQL(
      "INSERT OR REPLACE INTO `ratings_new` " +
        "SELECT e.id_show_tmdb, r.type, r.rating, COALESCE(r.season_number, e.season_number), " +
        "COALESCE(r.episode_number, e.episode_number), " +
        "r.rated_at, r.created_at, r.updated_at " +
        "FROM `ratings` r JOIN `episodes` e ON e.id_tmdb = r.id_tmdb " +
        "WHERE r.type = 'episode'",
    )

    connection.execSQL("DROP TABLE `ratings`")
    connection.execSQL("ALTER TABLE `ratings_new` RENAME TO `ratings`")
    connection.execSQL(
      "CREATE INDEX IF NOT EXISTS `index_ratings_id_tmdb_type` ON `ratings` (`id_tmdb`, `type`)",
    )
  }
}

/**
 * Adds the network_logo_path column to the shows table.
 */
val MIGRATION_44_45 = object : Migration(44, 45) {
  override fun migrate(connection: SQLiteConnection) {
    connection.execSQL("ALTER TABLE `shows` ADD COLUMN `network_logo_path` TEXT NOT NULL DEFAULT ''")
  }
}
