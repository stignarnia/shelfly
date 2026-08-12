package xyz.stignarnia.ui_base.common.sheets.context_menu.movie

import xyz.stignarnia.ui_base.common.sheets.context_menu.movie.helpers.MovieContextItem

data class MovieContextMenuUiState(
  val isLoading: Boolean? = null,
  val item: MovieContextItem? = null,
)
