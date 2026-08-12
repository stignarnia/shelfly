package xyz.stignarnia.data_local.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_movies_log")
data class MoviesSyncLog(
  @PrimaryKey @ColumnInfo(name = "id_movie_tmdb", defaultValue = "-1") val idTmdb: Long,
  @ColumnInfo(name = "synced_at", defaultValue = "0") val syncedAt: Long,
)
