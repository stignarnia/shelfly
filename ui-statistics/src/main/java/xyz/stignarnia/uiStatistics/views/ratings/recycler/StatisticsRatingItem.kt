package xyz.stignarnia.uiStatistics.views.ratings.recycler

import xyz.stignarnia.uiBase.common.ListItem
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.UserRating

data class StatisticsRatingItem(
  override val show: Show,
  override val image: Image,
  override val isLoading: Boolean,
  val rating: UserRating,
) : ListItem
