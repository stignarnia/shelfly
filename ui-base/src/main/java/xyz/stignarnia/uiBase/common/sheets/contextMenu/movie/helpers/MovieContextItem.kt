package xyz.stignarnia.uiBase.common.sheets.contextMenu.movie.helpers

import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.SpoilersSettings
import xyz.stignarnia.uiModel.Translation
import java.time.format.DateTimeFormatter

data class MovieContextItem(
  val movie: Movie,
  val image: Image,
  val translation: Translation?,
  val dateFormat: DateTimeFormatter?,
  val userRating: Int?,
  val isMyMovie: Boolean,
  val isWatchlist: Boolean,
  val isHidden: Boolean,
  val isPinnedTop: Boolean,
  val spoilers: SpoilersSettings,
) {
  fun isInCollection() = isHidden || isWatchlist || isMyMovie
}
