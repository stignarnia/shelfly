package com.michaldrabik.ui_movie.sections.related.recycler

import androidx.recyclerview.widget.DiffUtil

class RelatedItemDiffCallback : DiffUtil.ItemCallback<RelatedListItem>() {

  override fun areItemsTheSame(
    oldItem: RelatedListItem,
    newItem: RelatedListItem,
  ) = oldItem.movie.ids.tmdb == newItem.movie.ids.tmdb

  override fun areContentsTheSame(
    oldItem: RelatedListItem,
    newItem: RelatedListItem,
  ) = oldItem.image == newItem.image &&
    oldItem.isLoading == newItem.isLoading &&
    oldItem.isFollowed == newItem.isFollowed &&
    oldItem.isWatchlist == newItem.isWatchlist
}
