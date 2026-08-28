package xyz.stignarnia.uiStatisticsMovies.views.ratings.recycler

import androidx.recyclerview.widget.DiffUtil

class StatisticsMoviesRatingsDiffCallback : DiffUtil.ItemCallback<StatisticsMoviesRatingItem>() {
  override fun areItemsTheSame(
    oldItem: StatisticsMoviesRatingItem,
    newItem: StatisticsMoviesRatingItem,
  ) = oldItem.movie.ids.tmdb == newItem.movie.ids.tmdb

  override fun areContentsTheSame(
    oldItem: StatisticsMoviesRatingItem,
    newItem: StatisticsMoviesRatingItem,
  ) = oldItem.rating.rating == newItem.rating.rating &&
    oldItem.image == newItem.image
}
