package xyz.stignarnia.uiDiscover.recycler

import xyz.stignarnia.uiBase.common.ListItem
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.Translation

data class DiscoverListItem(
  override val show: Show,
  override val image: Image,
  override var isLoading: Boolean = false,
  val isFollowed: Boolean = false,
  val isWatchlist: Boolean = false,
  val translation: Translation? = null,
) : ListItem
