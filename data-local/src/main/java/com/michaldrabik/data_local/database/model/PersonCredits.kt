package com.michaldrabik.data_local.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.michaldrabik.data_local.database.converters.DateConverter
import java.time.ZonedDateTime

@Entity(
  tableName = "people_credits",
  foreignKeys = [
    ForeignKey(
      entity = Show::class,
      parentColumns = arrayOf("id_tmdb"),
      childColumns = arrayOf("id_tmdb_show"),
      onDelete = ForeignKey.CASCADE,
    ),
    ForeignKey(
      entity = Movie::class,
      parentColumns = arrayOf("id_tmdb"),
      childColumns = arrayOf("id_tmdb_movie"),
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [
    Index(value = ["id_tmdb_person"]),
    Index(value = ["id_tmdb_show"]),
    Index(value = ["id_tmdb_movie"]),
  ],
)
@TypeConverters(DateConverter::class)
data class PersonCredits(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long,
  @ColumnInfo(name = "id_tmdb_person") val idTmdbPerson: Long,
  @ColumnInfo(name = "id_tmdb_show") val idTmdbShow: Long?,
  @ColumnInfo(name = "id_tmdb_movie") val idTmdbMovie: Long?,
  @ColumnInfo(name = "type") val type: String,
  @ColumnInfo(name = "created_at") val createdAt: ZonedDateTime,
  @ColumnInfo(name = "updated_at") val updatedAt: ZonedDateTime,
)
