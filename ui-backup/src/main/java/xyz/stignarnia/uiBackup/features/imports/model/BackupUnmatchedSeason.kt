package xyz.stignarnia.uiBackup.features.imports.model

import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class BackupUnmatchedSeason(
  val seasonNumber: Int,
  val reason: BackupImportText? = null,
  val unmatchedEpisodes: List<BackupUnmatchedEpisode> = emptyList(),
) : Serializable {
  val isEntireSeasonUnmatched: Boolean
    get() = reason != null && unmatchedEpisodes.isEmpty()
}
