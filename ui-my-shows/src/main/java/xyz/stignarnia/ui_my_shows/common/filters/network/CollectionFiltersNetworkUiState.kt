package xyz.stignarnia.ui_my_shows.common.filters.network

import xyz.stignarnia.ui_model.Network

internal data class CollectionFiltersNetworkUiState(
  val networks: List<Network>? = null,
  val isLoading: Boolean? = null,
)
