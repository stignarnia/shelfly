package xyz.stignarnia.ui_my_shows.common.filters.network

internal data class CollectionFiltersNetworkUiState(
  val available: List<String>? = null,
  val selected: List<String> = emptyList(),
  val isLoading: Boolean? = null,
)
