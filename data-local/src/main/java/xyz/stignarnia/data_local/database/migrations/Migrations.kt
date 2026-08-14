package xyz.stignarnia.data_local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

const val DATABASE_VERSION = 43
const val DATABASE_NAME = "SHELFLY_DB"

/**
 * Adds the tombstone table backing multi-device sync.
 *
 * Purely additive, so unlike the jump to 42 this is a real migration rather
 * than a wipe: there is user data to preserve now.
 */
val MIGRATION_42_43 = object : Migration(42, 43) {
  override fun migrate(connection: SQLiteConnection) {
    connection.execSQL(
      "CREATE TABLE IF NOT EXISTS `sync_tombstones` (`entity_type` TEXT NOT NULL, `entity_key` TEXT NOT NULL, `deleted_at` INTEGER NOT NULL, PRIMARY KEY(`entity_type`, `entity_key`))",
    )
  }
}
