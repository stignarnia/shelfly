package xyz.stignarnia.data_local.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.TypeConverters
import xyz.stignarnia.data_local.database.converters.DateConverter
import java.time.ZonedDateTime

/**
 * A user rating for a show, movie, season or episode.
 *
 * [idTmdb] is always the TMDB id of the *top level* item - the show or the
 * movie. Seasons and episodes are then addressed by their number underneath it,
 * which is why both numbers are part of the key.
 *
 * That is the same identity the backup scheme and the sync keys use, so a
 * rating survives an export/import round trip and can be merged across devices.
 * Keying seasons and episodes by their own TMDB id, as this table did before
 * version 44, could not: a backup file never carried those ids.
 *
 * [NO_NUMBER] stands in for "not applicable" on show and movie ratings. SQLite
 * does not enforce NOT NULL on primary key columns of a rowid table, so a
 * nullable column here would let duplicate show ratings through.
 */
@Entity(
  tableName = "ratings",
  primaryKeys = ["id_tmdb", "type", "season_number", "episode_number"],
  indices = [
    Index(value = ["id_tmdb", "type"], unique = false),
  ],
)
@TypeConverters(DateConverter::class)
data class Rating(
  @ColumnInfo(name = "id_tmdb") val idTmdb: Long,
  @ColumnInfo(name = "type") val type: String,
  @ColumnInfo(name = "rating") val rating: Int,
  @ColumnInfo(name = "season_number") val seasonNumber: Int = NO_NUMBER,
  @ColumnInfo(name = "episode_number") val episodeNumber: Int = NO_NUMBER,
  @ColumnInfo(name = "rated_at") val ratedAt: ZonedDateTime,
  @ColumnInfo(name = "created_at") val createdAt: ZonedDateTime,
  @ColumnInfo(name = "updated_at") val updatedAt: ZonedDateTime,
) {

  companion object {
    const val NO_NUMBER = -1

    const val TYPE_SHOW = "show"
    const val TYPE_MOVIE = "movie"
    const val TYPE_SEASON = "season"
    const val TYPE_EPISODE = "episode"
  }
}
