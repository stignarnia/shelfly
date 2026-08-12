package xyz.stignarnia.ui_progress_movies.calendar.recycler

import androidx.recyclerview.widget.DiffUtil

class CalendarMovieItemDiffCallback : DiffUtil.ItemCallback<CalendarMovieListItem>() {

  override fun areItemsTheSame(
    oldItem: CalendarMovieListItem,
    newItem: CalendarMovieListItem,
  ): Boolean {
    if (oldItem is CalendarMovieListItem.Header && newItem is CalendarMovieListItem.Header) {
      return oldItem.textResId == newItem.textResId
    }
    if (oldItem is CalendarMovieListItem.MovieItem && newItem is CalendarMovieListItem.MovieItem) {
      return oldItem.movie.tmdbId == newItem.movie.tmdbId
    }
    if (oldItem is CalendarMovieListItem.Filters && newItem is CalendarMovieListItem.Filters) {
      return true
    }
    return false
  }

  override fun areContentsTheSame(
    oldItem: CalendarMovieListItem,
    newItem: CalendarMovieListItem,
  ): Boolean {
    if (oldItem is CalendarMovieListItem.Header && newItem is CalendarMovieListItem.Header) {
      return oldItem.textResId == newItem.textResId
    }
    if (oldItem is CalendarMovieListItem.MovieItem && newItem is CalendarMovieListItem.MovieItem) {
      return oldItem.image == newItem.image &&
        oldItem.isWatched == newItem.isWatched &&
        oldItem.isWatchlist == newItem.isWatchlist &&
        oldItem.isLoading == newItem.isLoading &&
        oldItem.translation == newItem.translation &&
        oldItem.spoilers == newItem.spoilers &&
        oldItem.movie == newItem.movie
    }
    if (oldItem is CalendarMovieListItem.Filters && newItem is CalendarMovieListItem.Filters) {
      return oldItem.mode == newItem.mode
    }
    return false
  }
}
