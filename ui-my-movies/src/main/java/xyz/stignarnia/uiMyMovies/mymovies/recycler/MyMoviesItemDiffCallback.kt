package xyz.stignarnia.uiMyMovies.mymovies.recycler

import androidx.recyclerview.widget.DiffUtil

class MyMoviesItemDiffCallback : DiffUtil.ItemCallback<MyMoviesItem>() {
  override fun areItemsTheSame(
    oldItem: MyMoviesItem,
    newItem: MyMoviesItem,
  ) = when (oldItem.type) {
    MyMoviesItem.Type.RECENT_MOVIES -> true
    else -> oldItem.type == newItem.type && oldItem.movie.ids.tmdb == newItem.movie.ids.tmdb
  }

  override fun areContentsTheSame(
    oldItem: MyMoviesItem,
    newItem: MyMoviesItem,
  ) = when (oldItem.type) {
    MyMoviesItem.Type.HEADER -> {
      oldItem.header == newItem.header
    }

    MyMoviesItem.Type.RECENT_MOVIES -> {
      oldItem.recentsSection == newItem.recentsSection
    }

    else -> {
      oldItem.image == newItem.image &&
        oldItem.isLoading == newItem.isLoading &&
        oldItem.translation == newItem.translation &&
        oldItem.spoilers == newItem.spoilers &&
        oldItem.userRating == newItem.userRating &&
        oldItem.sortOrder == newItem.sortOrder &&
        oldItem.dateFormat.toString() == newItem.dateFormat.toString()
    }
  }
}
