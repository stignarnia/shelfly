package com.michaldrabik.ui_statistics.views.mostWatched.recycler

import androidx.recyclerview.widget.DiffUtil
import com.michaldrabik.ui_statistics.views.mostWatched.StatisticsMostWatchedItem

class MostWatchedItemDiffCallback : DiffUtil.ItemCallback<StatisticsMostWatchedItem>() {

  override fun areItemsTheSame(
    oldItem: StatisticsMostWatchedItem,
    newItem: StatisticsMostWatchedItem,
  ) = oldItem.show.ids.tmdb == newItem.show.ids.tmdb

  override fun areContentsTheSame(
    oldItem: StatisticsMostWatchedItem,
    newItem: StatisticsMostWatchedItem,
  ) = oldItem.image == newItem.image &&
    oldItem.isLoading == newItem.isLoading &&
    oldItem.seasonsCount == newItem.seasonsCount &&
    oldItem.episodes.size == newItem.episodes.size &&
    oldItem.translation == newItem.translation
}
