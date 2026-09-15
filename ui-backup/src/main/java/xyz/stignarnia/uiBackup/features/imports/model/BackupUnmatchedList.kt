package xyz.stignarnia.uiBackup.features.imports.model

import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = true)
data class BackupUnmatchedList(
  val title: String,
  val reason: String? = null,
  val unmatchedItems: List<BackupUnmatchedItem> = emptyList(),
) : Serializable {
  val isEntireListUnmatched: Boolean
    get() = reason != null && unmatchedItems.isEmpty()
}
