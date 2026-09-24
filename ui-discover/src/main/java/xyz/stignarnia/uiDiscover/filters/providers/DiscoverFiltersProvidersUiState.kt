package xyz.stignarnia.uiDiscover.filters.providers

import xyz.stignarnia.uiBase.common.AppCountry
import xyz.stignarnia.uiModel.StreamingProvider

internal data class DiscoverFiltersProvidersUiState(
  val available: List<StreamingProvider>? = null,
  val selected: List<StreamingProvider> = emptyList(),
  val region: AppCountry? = null,
  val isLoading: Boolean = false,
  val isError: Boolean = false,
)
