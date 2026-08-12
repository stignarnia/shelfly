package com.michaldrabik.ui_statistics.views.ratings.recycler

import androidx.recyclerview.widget.DiffUtil

class StatisticsRatingsDiffCallback : DiffUtil.ItemCallback<StatisticsRatingItem>() {

  override fun areItemsTheSame(
    oldItem: StatisticsRatingItem,
    newItem: StatisticsRatingItem,
  ) = oldItem.show.ids.tmdb == newItem.show.ids.tmdb

  override fun areContentsTheSame(
    oldItem: StatisticsRatingItem,
    newItem: StatisticsRatingItem,
  ) = oldItem.rating.rating == newItem.rating.rating &&
    oldItem.image == newItem.image
}
