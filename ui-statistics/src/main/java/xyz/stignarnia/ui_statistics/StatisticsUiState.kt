package xyz.stignarnia.ui_statistics

import xyz.stignarnia.ui_model.Genre
import xyz.stignarnia.ui_statistics.views.mostWatched.StatisticsMostWatchedItem
import xyz.stignarnia.ui_statistics.views.ratings.recycler.StatisticsRatingItem

data class StatisticsUiState(
  val mostWatchedShows: List<StatisticsMostWatchedItem>? = null,
  val mostWatchedTotalCount: Int? = null,
  val totalTimeSpentMinutes: Int? = null,
  val totalWatchedEpisodes: Int? = null,
  val totalWatchedEpisodesShows: Int? = null,
  val topGenres: List<Genre>? = null,
  val ratings: List<StatisticsRatingItem>? = null,
)
