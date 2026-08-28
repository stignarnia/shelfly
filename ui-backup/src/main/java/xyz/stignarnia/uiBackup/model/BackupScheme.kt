package xyz.stignarnia.uiBackup.model

import com.squareup.moshi.JsonClass

/**
 * All dates should be exported in ISO 8601 format.
 * Example: "2021-01-01T00:00:00.000Z" -> YYYY-MM-DDTHH:mm:ss.sssZ
 */
@JsonClass(generateAdapter = true)
data class BackupScheme(
  val version: Int,
  val platform: String,
  val createdAt: String,
  val shows: BackupShows = BackupShows(),
  val movies: BackupMovies = BackupMovies(),
  val lists: BackupLists = BackupLists(),
)
