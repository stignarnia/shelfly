package xyz.stignarnia.ui_statistics_movies

import xyz.stignarnia.ui_model.Genre
import xyz.stignarnia.ui_statistics_movies.views.ratings.recycler.StatisticsMoviesRatingItem

data class StatisticsMoviesUiState(
  val totalTimeSpentMinutes: Int? = null,
  val totalWatchedMovies: Int? = null,
  val topGenres: List<Genre>? = null,
  val ratings: List<StatisticsMoviesRatingItem>? = null,
)
