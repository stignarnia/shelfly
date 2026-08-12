package xyz.stignarnia.ui_discover_movies.filters.genres

import xyz.stignarnia.ui_model.Genre

internal data class DiscoverMoviesFiltersGenresUiState(
  val genres: List<Genre>? = null,
  val isLoading: Boolean? = null,
)
