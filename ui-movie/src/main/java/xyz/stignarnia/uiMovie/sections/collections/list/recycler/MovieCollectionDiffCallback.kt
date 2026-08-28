package xyz.stignarnia.uiMovie.sections.collections.list.recycler

import androidx.recyclerview.widget.DiffUtil
import xyz.stignarnia.uiModel.MovieCollection

class MovieCollectionDiffCallback : DiffUtil.ItemCallback<MovieCollection>() {
  override fun areItemsTheSame(
    oldItem: MovieCollection,
    newItem: MovieCollection,
  ) = oldItem.id == newItem.id

  override fun areContentsTheSame(
    oldItem: MovieCollection,
    newItem: MovieCollection,
  ) = oldItem == newItem
}
