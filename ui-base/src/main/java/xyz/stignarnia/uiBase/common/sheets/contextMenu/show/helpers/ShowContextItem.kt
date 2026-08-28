package xyz.stignarnia.uiBase.common.sheets.contextMenu.show.helpers

import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.SpoilersSettings
import xyz.stignarnia.uiModel.Translation

data class ShowContextItem(
  val show: Show,
  val image: Image,
  val translation: Translation?,
  val userRating: Int?,
  val isMyShow: Boolean,
  val isWatchlist: Boolean,
  val isHidden: Boolean,
  val isPinnedTop: Boolean,
  val isOnHold: Boolean,
  val spoilers: SpoilersSettings,
) {
  fun isInCollection() = isHidden || isWatchlist || isMyShow
}
