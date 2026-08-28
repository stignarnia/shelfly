package xyz.stignarnia.uiShow.sections.seasons

import xyz.stignarnia.uiShow.sections.seasons.recycler.SeasonListItem

data class ShowDetailsSeasonsUiState(
  val isLoading: Boolean = true,
  val seasons: List<SeasonListItem>? = null,
)
