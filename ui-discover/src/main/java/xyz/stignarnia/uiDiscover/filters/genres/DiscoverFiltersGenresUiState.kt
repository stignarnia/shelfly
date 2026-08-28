package xyz.stignarnia.uiDiscover.filters.genres

import xyz.stignarnia.uiModel.Genre

internal data class DiscoverFiltersGenresUiState(
  val genres: List<Genre>? = null,
  val isLoading: Boolean? = null,
)
