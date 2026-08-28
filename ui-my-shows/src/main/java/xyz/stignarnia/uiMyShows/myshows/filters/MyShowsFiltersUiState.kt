package xyz.stignarnia.uiMyShows.myshows.filters

import xyz.stignarnia.uiModel.MyShowsSection

internal data class MyShowsFiltersUiState(
  val sectionType: MyShowsSection? = null,
  val isLoading: Boolean? = null,
)
