package xyz.stignarnia.uiProgressMovies.progress.recycler

import androidx.recyclerview.widget.DiffUtil

class ProgressMovieItemDiffCallback : DiffUtil.ItemCallback<ProgressMovieListItem>() {
  override fun areItemsTheSame(
    oldItem: ProgressMovieListItem,
    newItem: ProgressMovieListItem,
  ): Boolean {
    val areMovies = oldItem is ProgressMovieListItem.MovieItem && newItem is ProgressMovieListItem.MovieItem

    return when {
      areMovies -> {
        areItemsTheSame(
          (oldItem),
          (newItem),
        )
      }

      else -> {
        false
      }
    }
  }

  override fun areContentsTheSame(
    oldItem: ProgressMovieListItem,
    newItem: ProgressMovieListItem,
  ): Boolean =
    when (oldItem) {
      is ProgressMovieListItem.MovieItem -> {
        areContentsTheSame(oldItem, (newItem as ProgressMovieListItem.MovieItem))
      }

      is ProgressMovieListItem.HeaderItem -> {
        true
      }
    }

  private fun areItemsTheSame(
    oldItem: ProgressMovieListItem.MovieItem,
    newItem: ProgressMovieListItem.MovieItem,
  ): Boolean = oldItem.movie.ids.tmdb == newItem.movie.ids.tmdb

  private fun areContentsTheSame(
    oldItem: ProgressMovieListItem.MovieItem,
    newItem: ProgressMovieListItem.MovieItem,
  ): Boolean =
    oldItem.isPinned == newItem.isPinned &&
      oldItem.image == newItem.image &&
      oldItem.movie == newItem.movie &&
      oldItem.sortOrder == newItem.sortOrder &&
      oldItem.userRating == newItem.userRating &&
      oldItem.spoilers == newItem.spoilers &&
      oldItem.translation == newItem.translation
}
