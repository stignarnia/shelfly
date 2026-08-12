package com.michaldrabik.data_local.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey

@Entity(
  tableName = "shows_related",
  foreignKeys = [
    ForeignKey(
      entity = Show::class,
      parentColumns = arrayOf("id_tmdb"),
      childColumns = arrayOf("id_tmdb_related_show"),
      onDelete = CASCADE,
    ),
  ],
)
data class RelatedShow(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
  @ColumnInfo(name = "id_tmdb", defaultValue = "-1") val idTmdb: Long,
  @ColumnInfo(name = "id_tmdb_related_show", defaultValue = "-1", index = true) val idTmdbRelatedShow: Long,
  @ColumnInfo(name = "updated_at", defaultValue = "-1") val updatedAt: Long,
) {

  companion object {
    fun fromTmdbId(
      tmdbId: Long,
      relatedShowTmdbId: Long,
      nowUtcMillis: Long,
    ): RelatedShow =
      RelatedShow(
        idTmdb = tmdbId,
        idTmdbRelatedShow = relatedShowTmdbId,
        updatedAt = nowUtcMillis,
      )
  }
}
