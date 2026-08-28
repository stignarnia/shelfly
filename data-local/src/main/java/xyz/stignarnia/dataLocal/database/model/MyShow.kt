package xyz.stignarnia.dataLocal.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
  tableName = "shows_my_shows",
  foreignKeys = [
    ForeignKey(
      entity = Show::class,
      parentColumns = arrayOf("id_tmdb"),
      childColumns = arrayOf("id_tmdb"),
      onDelete = ForeignKey.CASCADE,
    ),
  ],
)
data class MyShow(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
  @ColumnInfo(name = "id_tmdb", defaultValue = "-1", index = true) val idTmdb: Long,
  @ColumnInfo(name = "created_at", defaultValue = "-1") val createdAt: Long,
  @ColumnInfo(name = "updated_at", defaultValue = "-1") val updatedAt: Long,
  @ColumnInfo(name = "last_watched_at") val lastWatchedAt: Long?,
) {
  companion object {
    fun fromTmdbId(
      tmdbId: Long,
      createdAt: Long,
      updatedAt: Long,
      watchedAt: Long,
    ) = MyShow(
      idTmdb = tmdbId,
      createdAt = createdAt,
      updatedAt = updatedAt,
      lastWatchedAt = watchedAt,
    )
  }
}
