package xyz.stignarnia.uiBackup.features.imports.model

import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class BackupUnmatchedItem(
  val title: String,
  val reason: String,
  val tmdbId: Long? = null,
) : Serializable
