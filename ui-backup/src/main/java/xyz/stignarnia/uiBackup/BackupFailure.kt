package xyz.stignarnia.uiBackup

import android.content.Context
import androidx.annotation.Keep
import androidx.annotation.StringRes
import xyz.stignarnia.dataWebdav.WebDavError
import kotlin.coroutines.cancellation.CancellationException

/**
 * Why a backup, a sync or a restore failed, in terms that can be told to the user.
 *
 * An exception's own message is never shown.
 * It is English whatever language the app is in, and usually written for a developer: "Job was cancelled", "Unexpected server response (507)".
 *
 * The name is what gets stored when a failure has to outlive the process, so renaming an entry turns previously recorded failures into [UNEXPECTED].
 * [Keep] stops R8 doing that renaming itself in release builds.
 */
@Keep
enum class BackupFailure(
  @field:StringRes val messageRes: Int,
) {
  UNAUTHORIZED(R.string.textWebDavErrorAuth),
  NOT_FOUND(R.string.textWebDavErrorNotFound),
  TLS(R.string.textWebDavErrorTls),
  UNREACHABLE(R.string.textWebDavErrorUnreachable),
  UNEXPECTED_RESPONSE(R.string.textWebDavErrorUnexpected),
  INTERRUPTED(R.string.textSyncNotificationInterruptedContent),
  UNEXPECTED(R.string.textBackupErrorUnexpected),
  ;

  companion object {
    fun of(error: Throwable): BackupFailure =
      when (error) {
        is WebDavError.Unauthorized -> UNAUTHORIZED
        is WebDavError.NotFound -> NOT_FOUND
        is WebDavError.TlsFailure -> TLS
        is WebDavError.Unreachable -> UNREACHABLE
        is WebDavError.UnexpectedResponse -> UNEXPECTED_RESPONSE
        is CancellationException -> INTERRUPTED
        else -> UNEXPECTED
      }

    /**
     * Reads back a failure stored by name.
     * Anything unrecognised is [UNEXPECTED], which covers the free text stored before failures were recorded by name.
     */
    fun fromName(name: String?): BackupFailure? = name?.let { stored -> entries.firstOrNull { it.name == stored } ?: UNEXPECTED }
  }
}

/**
 * A failure whose user-facing message is already known where it is raised.
 * [cause] keeps whatever went wrong underneath for the log, since none of it is shown.
 */
class BackupException(
  @field:StringRes val messageRes: Int,
  vararg val formatArgs: Any,
  cause: Throwable? = null,
) : Exception(cause)

/**
 * What to tell the user about this error, in the app's language.
 */
fun Throwable.describe(context: Context): String =
  when (this) {
    is BackupException -> context.getString(messageRes, *formatArgs)
    else -> context.getString(BackupFailure.of(this).messageRes)
  }
