package xyz.stignarnia.ui_show.sections.seasons

import xyz.stignarnia.ui_show.sections.seasons.recycler.SeasonListItem

data class ShowDetailsSeasonsUiState(
  val isLoading: Boolean = true,
  val seasons: List<SeasonListItem>? = null,
)
