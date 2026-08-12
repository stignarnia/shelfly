package com.michaldrabik.ui_model

data class RatingState(
  val userRating: UserRating? = null,
  val rateLoading: Boolean? = null,
) {

  fun hasRating() = userRating != null && userRating.rating > 0
}
