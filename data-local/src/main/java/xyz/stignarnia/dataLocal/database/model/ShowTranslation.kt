package xyz.stignarnia.dataLocal.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "shows_translations",
  indices = [Index(value = ["id_tmdb"], unique = true)],
  foreignKeys = [
    ForeignKey(
      entity = Show::class,
      parentColumns = arrayOf("id_tmdb"),
      childColumns = arrayOf("id_tmdb"),
      onDelete = ForeignKey.CASCADE,
    ),
  ],
)
data class ShowTranslation(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
  @ColumnInfo(name = "id_tmdb") val idTmdb: Long,
  @ColumnInfo(name = "title") val title: String,
  @ColumnInfo(name = "language") val language: String,
  @ColumnInfo(name = "overview") val overview: String,
  @ColumnInfo(name = "created_at") val createdAt: Long,
  @ColumnInfo(name = "updated_at") val updatedAt: Long,
) {
  companion object {
    fun fromTmdbId(
      tmdbId: Long,
      title: String,
      language: String,
      overview: String,
      createdAt: Long,
    ) = ShowTranslation(
      idTmdb = tmdbId,
      title = title,
      language = language,
      overview = overview,
      createdAt = createdAt,
      updatedAt = createdAt,
    )
  }
}
