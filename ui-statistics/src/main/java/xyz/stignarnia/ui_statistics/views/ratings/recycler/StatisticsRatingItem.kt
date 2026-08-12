package xyz.stignarnia.ui_statistics.views.ratings.recycler

import xyz.stignarnia.ui_base.common.ListItem
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_model.UserRating

data class StatisticsRatingItem(
  override val show: Show,
  override val image: Image,
  override val isLoading: Boolean,
  val rating: UserRating,
) : ListItem
