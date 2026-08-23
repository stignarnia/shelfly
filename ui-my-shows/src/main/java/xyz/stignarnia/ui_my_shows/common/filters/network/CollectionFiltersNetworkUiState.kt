package xyz.stignarnia.ui_my_shows.common.filters.network

import xyz.stignarnia.ui_model.StreamingProvider

internal data class CollectionFiltersNetworkUiState(
  val available: List<StreamingProvider>? = null,
  val selected: List<String> = emptyList(),
  val isLoading: Boolean? = null,
)
