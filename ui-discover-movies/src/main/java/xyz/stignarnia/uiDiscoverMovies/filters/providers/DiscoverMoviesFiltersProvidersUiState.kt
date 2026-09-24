package xyz.stignarnia.uiDiscoverMovies.filters.providers

import xyz.stignarnia.uiBase.common.AppCountry
import xyz.stignarnia.uiModel.StreamingProvider

internal data class DiscoverMoviesFiltersProvidersUiState(
  val available: List<StreamingProvider>? = null,
  val selected: List<StreamingProvider> = emptyList(),
  val region: AppCountry? = null,
  val isLoading: Boolean = false,
  val isError: Boolean = false,
)
