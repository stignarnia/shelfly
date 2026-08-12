package xyz.stignarnia.ui_show.sections.related.recycler

import xyz.stignarnia.ui_base.common.ListItem
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.Show

data class RelatedListItem(
  override val show: Show,
  override val image: Image,
  override var isLoading: Boolean = false,
  val isFollowed: Boolean = false,
  val isWatchlist: Boolean = false,
) : ListItem
