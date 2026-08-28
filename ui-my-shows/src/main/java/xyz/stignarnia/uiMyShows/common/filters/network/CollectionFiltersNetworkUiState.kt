package xyz.stignarnia.uiMyShows.common.filters.network

import xyz.stignarnia.uiModel.StreamingProvider

internal data class CollectionFiltersNetworkUiState(
  val available: List<StreamingProvider>? = null,
  val selected: List<String> = emptyList(),
  val isLoading: Boolean? = null,
)
