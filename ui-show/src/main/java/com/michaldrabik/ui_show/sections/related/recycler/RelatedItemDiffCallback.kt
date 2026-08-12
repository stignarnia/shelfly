package com.michaldrabik.ui_show.sections.related.recycler

import androidx.recyclerview.widget.DiffUtil

class RelatedItemDiffCallback : DiffUtil.ItemCallback<RelatedListItem>() {

  override fun areItemsTheSame(
    oldItem: RelatedListItem,
    newItem: RelatedListItem,
  ) = oldItem.show.ids.tmdb == newItem.show.ids.tmdb

  override fun areContentsTheSame(
    oldItem: RelatedListItem,
    newItem: RelatedListItem,
  ) = oldItem.image == newItem.image &&
    oldItem.isLoading == newItem.isLoading &&
    oldItem.isFollowed == newItem.isFollowed &&
    oldItem.isWatchlist == newItem.isWatchlist
}
