package xyz.stignarnia.ui_my_shows.myshows.filters

import xyz.stignarnia.ui_model.MyShowsSection

internal data class MyShowsFiltersUiState(
  val sectionType: MyShowsSection? = null,
  val isLoading: Boolean? = null,
)
