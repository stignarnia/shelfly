package xyz.stignarnia.uiMovie.sections.ratings

import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.Ratings

data class MovieDetailsRatingsUiState(
  val movie: Movie? = null,
  val ratings: Ratings? = null,
  val isRefreshingRatings: Boolean = false,
)
