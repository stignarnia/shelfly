package xyz.stignarnia.uiMyMovies.filters.genre

import xyz.stignarnia.uiModel.Genre

internal data class CollectionFiltersGenreUiState(
  val genres: List<Genre>? = null,
  val isLoading: Boolean? = null,
)
