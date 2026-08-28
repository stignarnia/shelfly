package xyz.stignarnia.uiMyShows.common.filters.genre

import xyz.stignarnia.uiModel.Genre

internal data class CollectionFiltersGenreUiState(
  val genres: List<Genre>? = null,
  val isLoading: Boolean? = null,
)
