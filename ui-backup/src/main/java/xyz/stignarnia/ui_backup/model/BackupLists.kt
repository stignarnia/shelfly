package xyz.stignarnia.ui_backup.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupLists(
  @param:Json(name = "l") val lists: List<BackupList> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class BackupList(
  @param:Json(name = "id") val id: Long,
  @param:Json(name = "sId") val slugId: String,
  @param:Json(name = "n") val name: String,
  @param:Json(name = "d") val description: String?,
  @param:Json(name = "p") val privacy: String,
  @param:Json(name = "ic") val itemCount: Long,
  @param:Json(name = "c") val createdAt: String,
  @param:Json(name = "u") val updatedAt: String,
  @param:Json(name = "it") val items: List<BackupListItem> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class BackupListItem(
  @param:Json(name = "id") val id: Long,
  @param:Json(name = "lId") val listId: Long,
  @param:Json(name = "tmId") val tmdbId: Long,
  @param:Json(name = "t") val type: String,
  @param:Json(name = "r") val rank: Long,
  @param:Json(name = "l") val listedAt: String,
  @param:Json(name = "c") val createdAt: String,
  @param:Json(name = "u") val updatedAt: String,
)
