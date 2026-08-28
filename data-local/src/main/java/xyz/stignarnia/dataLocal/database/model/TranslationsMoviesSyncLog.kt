package xyz.stignarnia.dataLocal.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_movies_translations_log")
data class TranslationsMoviesSyncLog(
  @PrimaryKey @ColumnInfo(name = "id_movie_tmdb") val idTmdb: Long,
  @ColumnInfo(name = "synced_at") val syncedAt: Long,
)
