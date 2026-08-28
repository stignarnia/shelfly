package xyz.stignarnia.uiDiscoverMovies.filters.genres

import xyz.stignarnia.uiModel.Genre

internal data class DiscoverMoviesFiltersGenresUiState(
  val genres: List<Genre>? = null,
  val isLoading: Boolean? = null,
)
