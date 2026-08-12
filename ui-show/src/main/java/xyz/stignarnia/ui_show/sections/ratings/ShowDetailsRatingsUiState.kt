package xyz.stignarnia.ui_show.sections.ratings

import xyz.stignarnia.ui_model.Ratings
import xyz.stignarnia.ui_model.Show

data class ShowDetailsRatingsUiState(
  val show: Show? = null,
  val ratings: Ratings? = null,
  val isRefreshingRatings: Boolean = false,
)
