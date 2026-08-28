package xyz.stignarnia.uiDiscover.filters.providers

import xyz.stignarnia.uiModel.StreamingProvider

internal data class DiscoverFiltersProvidersUiState(
  val available: List<StreamingProvider>? = null,
  val selected: List<StreamingProvider> = emptyList(),
  val regionName: String = "",
  val isLoading: Boolean = false,
  val isError: Boolean = false,
)
