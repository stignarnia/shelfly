package xyz.stignarnia.ui_discover.filters.providers

import xyz.stignarnia.ui_model.StreamingProvider

internal data class DiscoverFiltersProvidersUiState(
  val available: List<StreamingProvider>? = null,
  val selected: List<StreamingProvider> = emptyList(),
  val regionName: String = "",
  val isLoading: Boolean = false,
  val isError: Boolean = false,
)
