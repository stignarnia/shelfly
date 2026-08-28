package xyz.stignarnia.uiStatisticsMovies

import xyz.stignarnia.uiModel.Genre
import xyz.stignarnia.uiStatisticsMovies.views.ratings.recycler.StatisticsMoviesRatingItem

data class StatisticsMoviesUiState(
  val totalTimeSpentMinutes: Int? = null,
  val totalWatchedMovies: Int? = null,
  val topGenres: List<Genre>? = null,
  val ratings: List<StatisticsMoviesRatingItem>? = null,
)
