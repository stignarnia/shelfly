package com.michaldrabik.data_local.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "movies_translations",
  indices = [Index(value = ["id_tmdb"], unique = true)],
  foreignKeys = [
    ForeignKey(
      entity = Movie::class,
      parentColumns = arrayOf("id_tmdb"),
      childColumns = arrayOf("id_tmdb"),
      onDelete = ForeignKey.CASCADE,
    ),
  ],
)
data class MovieTranslation(
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
    ) = MovieTranslation(
      idTmdb = tmdbId,
      title = title,
      language = language,
      overview = overview,
      createdAt = createdAt,
      updatedAt = createdAt,
    )
  }
}
