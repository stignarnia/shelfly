package xyz.stignarnia.uiBackup.features.imports.model

import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class BackupUnmatchedItem(
  val title: BackupImportText,
  val reason: BackupImportText,
  val tmdbId: Long? = null,
) : Serializable
