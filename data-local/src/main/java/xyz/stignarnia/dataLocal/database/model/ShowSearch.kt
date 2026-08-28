package xyz.stignarnia.dataLocal.database.model

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

data class ShowSearch(
  @PrimaryKey @ColumnInfo(name = "id_tmdb") val idTmdb: Long,
  @ColumnInfo(name = "title") val title: String,
)
