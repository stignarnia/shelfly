package xyz.stignarnia.uiStatistics

import xyz.stignarnia.uiModel.Genre
import xyz.stignarnia.uiStatistics.views.mostWatched.StatisticsMostWatchedItem
import xyz.stignarnia.uiStatistics.views.ratings.recycler.StatisticsRatingItem

data class StatisticsUiState(
  val mostWatchedShows: List<StatisticsMostWatchedItem>? = null,
  val mostWatchedTotalCount: Int? = null,
  val totalTimeSpentMinutes: Int? = null,
  val totalWatchedEpisodes: Int? = null,
  val totalWatchedEpisodesShows: Int? = null,
  val topGenres: List<Genre>? = null,
  val ratings: List<StatisticsRatingItem>? = null,
)
