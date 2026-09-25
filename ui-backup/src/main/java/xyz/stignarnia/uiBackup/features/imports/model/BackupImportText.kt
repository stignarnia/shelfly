package xyz.stignarnia.uiBackup.features.imports.model

import android.content.Context
import androidx.annotation.StringRes
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import xyz.stignarnia.uiBackup.R
import java.io.Serializable
import java.lang.reflect.Type

/**
 * A title or reason in the import report, kept as a message and its arguments rather than as finished text.
 *
 * The report is saved and can be reopened long after the import, possibly after the user has switched language, so it is only turned into words when it is shown.
 * A [message] of null is text that needs no translating - a title taken from the backup - carried as the single argument.
 *
 * The message is stored by name, so renaming an entry of [Message] makes a saved report unreadable.
 */
@JsonClass(generateAdapter = true)
data class BackupImportText(
  val message: Message? = null,
  val args: List<String> = emptyList(),
) : Serializable {
  /**
   * generateAdapter = false keeps Moshi's built-in enum adapter, which serialises by constant name.
   * The annotation is still needed: Moshi's shipped R8 rules keep the constant names of annotated enums only, and a release build could otherwise rename them between versions, leaving the saved report unreadable after an update.
   */
  @JsonClass(generateAdapter = false)
  enum class Message(
    @field:StringRes val res: Int,
  ) {
    TITLE_BLANK(R.string.textBackupReasonTitleBlank),
    NO_RESULTS(R.string.textBackupReasonNoResults),
    ONLY_MOVIES_FOUND(R.string.textBackupReasonOnlyMoviesFound),
    ONLY_SHOWS_FOUND(R.string.textBackupReasonOnlyShowsFound),
    NO_EXACT_MATCH(R.string.textBackupReasonNoExactMatch),
    INVALID_TMDB_ID(R.string.textBackupReasonInvalidTmdbId),
    LOOKUP_FAILED(R.string.textBackupReasonLookupFailed),
    TMDB_API_ERROR(R.string.textBackupReasonTmdbApiError),
    TMDB_NETWORK_ERROR(R.string.textBackupReasonTmdbNetworkError),
    DETAILS_NOT_FOUND(R.string.textBackupReasonDetailsNotFound),
    DETAILS_FAILED(R.string.textBackupReasonDetailsFailed),
    SEASONS_NOT_FOUND(R.string.textBackupReasonSeasonsNotFound),
    SEASONS_FAILED(R.string.textBackupReasonSeasonsFailed),
    SEASON_NOT_FOUND(R.string.textBackupUnmatchedSeasonMissing),
    EPISODE_NOT_FOUND(R.string.textBackupReasonEpisodeNotFound),
    SEASON_NOT_FOUND_ANYWHERE(R.string.textBackupReasonSeasonNotFoundAnywhere),
    EPISODE_NOT_FOUND_ANYWHERE(R.string.textBackupReasonEpisodeNotFoundAnywhere),
    SHOW_UNAVAILABLE(R.string.textBackupReasonShowUnavailable),
    SEASON_UNAVAILABLE(R.string.textBackupReasonSeasonUnavailable),
    ORPHAN_SHOW(R.string.textBackupReasonOrphanShow),
    NOT_IN_COLLECTION(R.string.textBackupReasonNotInCollection),
    LIST_CREATE_FAILED(R.string.textBackupReasonListCreateFailed),
    LIST_MERGED(R.string.textBackupReasonListMerged),
    ADD_TO_LIST_FAILED(R.string.textBackupReasonAddToListFailed),
    SHOW_LEGACY_ID(R.string.textBackupTitleShowLegacyId),
    MOVIE_LEGACY_ID(R.string.textBackupTitleMovieLegacyId),
    ITEM_LEGACY_ID(R.string.textBackupTitleItemLegacyId),
    SHOW_TMDB_ID(R.string.textBackupTitleShowTmdbId),
    MOVIE_TMDB_ID(R.string.textBackupTitleMovieTmdbId),
    ITEM_TMDB_ID(R.string.textBackupTitleItemTmdbId),
  }

  fun resolve(context: Context): String =
    when (message) {
      null -> args.firstOrNull().orEmpty()
      else -> context.getString(message.res, *args.toTypedArray())
    }

  companion object {
    fun of(
      message: Message,
      vararg args: Any,
    ) = BackupImportText(message, args.map { it.toString() })

    /** Text shown as it is, such as a title from the backup. */
    fun verbatim(text: String) = BackupImportText(args = listOf(text))
  }

  /**
   * Reads a report saved before its text was stored this way, when every title and reason was a plain string.
   * Such a string is kept verbatim: the language it was written in is unknown, so there is nothing to translate it from.
   *
   * Implemented as [JsonAdapter.Factory] instead of reflective @FromJson/@ToJson methods so R8 minification
   * cannot strip generic signatures at runtime.
   */
  object LegacyAdapter : JsonAdapter.Factory {
    override fun create(
      type: Type,
      annotations: Set<Annotation>,
      moshi: Moshi,
    ): JsonAdapter<*>? {
      if (Types.getRawType(type) != BackupImportText::class.java) return null
      val delegate = moshi.nextAdapter<BackupImportText>(this, type, annotations)
      return object : JsonAdapter<BackupImportText>() {
        override fun fromJson(reader: JsonReader): BackupImportText? =
          when (reader.peek()) {
            JsonReader.Token.STRING -> verbatim(reader.nextString())
            else -> delegate.fromJson(reader)
          }

        override fun toJson(writer: JsonWriter, value: BackupImportText?) {
          delegate.toJson(writer, value)
        }
      }
    }
  }
}
