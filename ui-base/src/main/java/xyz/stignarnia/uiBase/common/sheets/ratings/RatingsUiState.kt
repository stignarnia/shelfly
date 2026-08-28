package xyz.stignarnia.uiBase.common.sheets.ratings

import xyz.stignarnia.uiModel.UserRating

data class RatingsUiState(
  val isLoading: Boolean? = null,
  val rating: UserRating? = null,
)
