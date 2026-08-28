package xyz.stignarnia.uiShow.sections.ratings

import xyz.stignarnia.uiModel.Ratings
import xyz.stignarnia.uiModel.Show

data class ShowDetailsRatingsUiState(
  val show: Show? = null,
  val ratings: Ratings? = null,
  val isRefreshingRatings: Boolean = false,
)
