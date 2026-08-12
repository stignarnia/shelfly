package xyz.stignarnia.ui_movie.sections.ratings

import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.Ratings

data class MovieDetailsRatingsUiState(
  val movie: Movie? = null,
  val ratings: Ratings? = null,
  val isRefreshingRatings: Boolean = false,
)
