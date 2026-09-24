package xyz.stignarnia.uiBackup.features.export

import xyz.stignarnia.common.extensions.nowUtc
import xyz.stignarnia.common.extensions.toLocalZone
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object BackupFileName {
  const val PREFIX = "shelfly_export_"

  /** Backups written before the rename still carry the old prefix. */
  const val LEGACY_PREFIX = "showly_export_"
  const val FILE_TYPE = ".json"
  const val DATE_TIME_PATTERN = "yyyyMMddHHmmss"
  const val MIME_TYPE = "application/json"

  private val dateFormat = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)

  fun create(): String {
    val currentDate = nowUtc().toLocalZone()
    return PREFIX + dateFormat.format(currentDate) + FILE_TYPE
  }

  /**
   * The local time a backup was written at, read back out of a name [create] produced.
   * Null for any name not in that shape.
   */
  fun parseDate(fileName: String): LocalDateTime? {
    val stamp =
      listOf(PREFIX, LEGACY_PREFIX)
        .firstOrNull { fileName.startsWith(it) }
        ?.let { fileName.removePrefix(it).removeSuffix(FILE_TYPE) }
        ?: return null
    return try {
      LocalDateTime.parse(stamp, dateFormat)
    } catch (_: DateTimeParseException) {
      null
    }
  }
}
