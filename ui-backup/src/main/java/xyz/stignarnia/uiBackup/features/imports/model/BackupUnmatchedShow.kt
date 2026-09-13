package xyz.stignarnia.uiBackup.features.imports.model

import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class BackupUnmatchedShow(
  val title: String,
  val reason: String? = null,
  val tmdbId: Long? = null,
  val unmatchedSeasons: List<BackupUnmatchedSeason> = emptyList(),
) : Serializable {
  val isEntireShowUnmatched: Boolean
    get() = reason != null && unmatchedSeasons.isEmpty()
}
