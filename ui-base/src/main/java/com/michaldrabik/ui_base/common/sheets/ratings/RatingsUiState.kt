package com.michaldrabik.ui_base.common.sheets.ratings

import com.michaldrabik.ui_model.UserRating

data class RatingsUiState(
  val isLoading: Boolean? = null,
  val rating: UserRating? = null,
)
