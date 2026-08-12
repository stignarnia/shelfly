package xyz.stignarnia.ui_my_shows.common.filters.genre

import xyz.stignarnia.ui_model.Genre

internal data class CollectionFiltersGenreUiState(
  val genres: List<Genre>? = null,
  val isLoading: Boolean? = null,
)
