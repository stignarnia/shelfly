package xyz.stignarnia.ui_base.common.sheets.context_menu.show.helpers

import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_model.SpoilersSettings
import xyz.stignarnia.ui_model.Translation

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
