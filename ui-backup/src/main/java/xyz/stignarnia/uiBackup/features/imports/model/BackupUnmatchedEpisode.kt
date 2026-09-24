package xyz.stignarnia.uiBackup.features.imports.model

import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class BackupUnmatchedEpisode(
  val episodeNumber: Int,
  val seasonNumber: Int,
  val title: String? = null,
  val reason: BackupImportText,
) : Serializable
