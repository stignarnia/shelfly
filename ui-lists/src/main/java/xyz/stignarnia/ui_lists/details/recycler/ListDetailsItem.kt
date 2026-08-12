package xyz.stignarnia.ui_lists.details.recycler

import xyz.stignarnia.common.extensions.toMillis
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SpoilersSettings
import xyz.stignarnia.ui_model.Translation
import java.time.ZoneOffset
import java.time.ZonedDateTime

data class ListDetailsItem(
  val id: Long,
  val rank: Long,
  val rankDisplay: Int,
  val show: Show?,
  val movie: Movie?,
  val image: Image,
  val translation: Translation?,
  val userRating: Int?,
  val isLoading: Boolean,
  val isRankDisplayed: Boolean,
  val isManageMode: Boolean,
  val isEnabled: Boolean,
  val isWatched: Boolean,
  val isWatchlist: Boolean,
  val listedAt: ZonedDateTime,
  val sortOrder: SortOrder,
  val spoilers: SpoilersSettings,
) {

  fun getTitleNoThe(): String {
    if (isShow()) return requireShow().titleNoThe
    if (isMovie()) return requireMovie().titleNoThe
    throw IllegalStateException()
  }

  fun getYear(): Int {
    if (isShow()) return requireShow().year
    if (isMovie()) return requireMovie().year
    throw IllegalStateException()
  }

  fun getDate(): Long {
    if (isShow()) {
      return if (requireShow().firstAired.isBlank()) 0 else ZonedDateTime.parse(requireShow().firstAired).toMillis()
    }
    if (isMovie()) {
      return requireMovie()
        .released
        ?.atStartOfDay()
        ?.toInstant(ZoneOffset.UTC)
        ?.toEpochMilli() ?: 0
    }
    throw IllegalStateException()
  }

  fun getRating(): Float {
    if (isShow()) return requireShow().rating
    if (isMovie()) return requireMovie().rating
    throw IllegalStateException()
  }

  fun getTmdbId(): IdTmdb {
    if (isShow()) return IdTmdb(requireShow().tmdbId)
    if (isMovie()) return IdTmdb(requireMovie().tmdbId)
    throw IllegalStateException()
  }

  fun isShow() = show != null

  fun isMovie() = movie != null

  fun requireShow() = show!!

  fun requireMovie() = movie!!
}
