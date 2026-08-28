package xyz.stignarnia.uiShow.sections.related.recycler

import xyz.stignarnia.uiBase.common.ListItem
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.Show

data class RelatedListItem(
  override val show: Show,
  override val image: Image,
  override var isLoading: Boolean = false,
  val isFollowed: Boolean = false,
  val isWatchlist: Boolean = false,
) : ListItem
