package xyz.stignarnia.ui_discover.filters.genres

import xyz.stignarnia.ui_model.Genre

internal data class DiscoverFiltersGenresUiState(
  val genres: List<Genre>? = null,
  val isLoading: Boolean? = null,
)
