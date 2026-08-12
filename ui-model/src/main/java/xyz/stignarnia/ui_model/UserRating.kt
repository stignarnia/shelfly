package xyz.stignarnia.ui_model

import xyz.stignarnia.common.extensions.nowUtc
import java.time.ZonedDateTime

data class UserRating(
  val idTmdb: IdTmdb,
  val rating: Int,
  val ratedAt: ZonedDateTime,
) {
  companion object {
    val EMPTY = UserRating(IdTmdb(-1), 0, nowUtc())
  }
}
