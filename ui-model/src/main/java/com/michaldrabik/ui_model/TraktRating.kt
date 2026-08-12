package com.michaldrabik.ui_model

import com.michaldrabik.common.extensions.nowUtc
import java.time.ZonedDateTime

data class TraktRating(
  val idTmdb: IdTmdb,
  val rating: Int,
  val ratedAt: ZonedDateTime,
) {
  companion object {
    val EMPTY = TraktRating(IdTmdb(-1), 0, nowUtc())
  }
}
