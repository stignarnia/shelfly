package xyz.stignarnia.uiModel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import xyz.stignarnia.common.extensions.nowUtc
import java.time.ZonedDateTime

@Parcelize
data class Episode(
  val season: Int,
  val number: Int,
  val title: String,
  val ids: Ids,
  val overview: String,
  val rating: Float,
  val votes: Int,
  val commentCount: Int,
  val firstAired: ZonedDateTime?,
  val runtime: Int,
  val numberAbs: Int?,
  val lastWatchedAt: ZonedDateTime?,
) : Parcelable {
  companion object {
    val EMPTY = Episode(-1, -1, "", Ids.EMPTY, "", -1F, -1, -1, null, -1, -1, null)
  }

  fun hasAired(season: Season): Boolean {
    val nowUtc = nowUtc()
    return when (firstAired) {
      null -> {
        season.episodes.any {
          it.number > number && (it.firstAired != null && nowUtc.isAfter(it.firstAired))
        }
      }

      else -> {
        nowUtc.isAfter(firstAired)
      }
    }
  }
}
