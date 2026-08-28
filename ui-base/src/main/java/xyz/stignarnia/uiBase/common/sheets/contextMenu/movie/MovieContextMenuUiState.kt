package xyz.stignarnia.uiBase.common.sheets.contextMenu.movie

import xyz.stignarnia.uiBase.common.sheets.contextMenu.movie.helpers.MovieContextItem

data class MovieContextMenuUiState(
  val isLoading: Boolean? = null,
  val item: MovieContextItem? = null,
)
