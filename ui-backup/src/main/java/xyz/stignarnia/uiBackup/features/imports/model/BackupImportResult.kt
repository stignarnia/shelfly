package xyz.stignarnia.uiBackup.features.imports.model

import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class BackupImportResult(
  val importedMoviesCount: Int,
  val importedShowsCount: Int,
  val unmatchedMovies: List<BackupUnmatchedItem> = emptyList(),
  val unmatchedShows: List<BackupUnmatchedShow> = emptyList(),
) : Serializable
