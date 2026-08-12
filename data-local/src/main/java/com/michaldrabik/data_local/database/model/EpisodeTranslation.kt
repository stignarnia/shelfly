package com.michaldrabik.data_local.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "episodes_translations",
  indices = [
    Index(value = ["id_tmdb"], unique = true),
    Index(value = ["id_tmdb_show"]),
  ],
  foreignKeys = [
    ForeignKey(
      entity = Show::class,
      parentColumns = arrayOf("id_tmdb"),
      childColumns = arrayOf("id_tmdb_show"),
      onDelete = ForeignKey.CASCADE,
    ),
  ],
)
data class EpisodeTranslation(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
  @ColumnInfo(name = "id_tmdb") val idTmdb: Long,
  @ColumnInfo(name = "id_tmdb_show") val idTmdbShow: Long,
  @ColumnInfo(name = "title") val title: String,
  @ColumnInfo(name = "language") val language: String,
  @ColumnInfo(name = "overview") val overview: String,
  @ColumnInfo(name = "created_at") val createdAt: Long,
  @ColumnInfo(name = "updated_at") val updatedAt: Long,
) {

  companion object {
    fun fromTmdbId(
      traktEpisodeId: Long,
      traktShowId: Long,
      title: String,
      language: String,
      overview: String,
      createdAt: Long,
    ) = EpisodeTranslation(
      idTmdb = traktEpisodeId,
      idTmdbShow = traktShowId,
      title = title,
      language = language,
      overview = overview,
      createdAt = createdAt,
      updatedAt = createdAt,
    )
  }
}
