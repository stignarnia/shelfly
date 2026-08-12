package xyz.stignarnia.ui_discover.recycler

import xyz.stignarnia.ui_base.common.ListItem
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_model.Translation

data class DiscoverListItem(
  override val show: Show,
  override val image: Image,
  override var isLoading: Boolean = false,
  val isFollowed: Boolean = false,
  val isWatchlist: Boolean = false,
  val translation: Translation? = null,
) : ListItem
