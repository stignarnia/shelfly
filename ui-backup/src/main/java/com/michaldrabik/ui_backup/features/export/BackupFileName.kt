package com.michaldrabik.ui_backup.features.export

import com.michaldrabik.common.extensions.nowUtc
import com.michaldrabik.common.extensions.toLocalZone
import java.time.format.DateTimeFormatter

object BackupFileName {

  val prefix = "shelfly_export_"

  /** Backups written before the rename still carry the old prefix. */
  val legacyPrefix = "showly_export_"
  val fileType = ".json"
  val dateTimePattern = "yyyyMMddHHmmss"
  val memeType = "application/json"

  fun create(): String {
    val dateFormat = DateTimeFormatter.ofPattern(dateTimePattern)
    val currentDate = nowUtc().toLocalZone()
    return prefix + dateFormat.format(currentDate) + fileType
  }
}
