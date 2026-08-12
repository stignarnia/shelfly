package xyz.stignarnia.ui_base.common.sheets.ratings

import xyz.stignarnia.ui_model.UserRating

data class RatingsUiState(
  val isLoading: Boolean? = null,
  val rating: UserRating? = null,
)
