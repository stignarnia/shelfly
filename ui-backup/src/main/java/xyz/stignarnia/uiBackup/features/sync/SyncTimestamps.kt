package xyz.stignarnia.uiBackup.features.sync

/**
 * Backup timestamps are ISO-8601 strings.
 * An unparseable or absent one becomes 0, which orders it behind everything rather than failing the whole sync for one malformed field.
 */
internal fun String?.toEpochMillis(): Long {
  if (this.isNullOrBlank()) return 0
  return try {
    java.time.Instant
      .parse(this)
      .toEpochMilli()
  } catch (error: Exception) {
    try {
      java.time.ZonedDateTime
        .parse(this)
        .toInstant()
        .toEpochMilli()
    } catch (error: Exception) {
      0
    }
  }
}
