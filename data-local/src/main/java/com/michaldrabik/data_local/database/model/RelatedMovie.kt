package com.michaldrabik.data_local.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey

@Entity(
  tableName = "movies_related",
  foreignKeys = [
    ForeignKey(
      entity = Movie::class,
      parentColumns = arrayOf("id_tmdb"),
      childColumns = arrayOf("id_tmdb_related_movie"),
      onDelete = CASCADE,
    ),
  ],
)
data class RelatedMovie(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
  @ColumnInfo(name = "id_tmdb", defaultValue = "-1") val idTmdb: Long,
  @ColumnInfo(name = "id_tmdb_related_movie", defaultValue = "-1", index = true) val idTmdbRelatedMovie: Long,
  @ColumnInfo(name = "updated_at", defaultValue = "-1") val updatedAt: Long,
) {

  companion object {
    fun fromTmdbId(
      tmdbId: Long,
      relatedTmdbId: Long,
      nowUtcMillis: Long,
    ) = RelatedMovie(
      idTmdb = tmdbId,
      idTmdbRelatedMovie = relatedTmdbId,
      updatedAt = nowUtcMillis,
    )
  }
}
