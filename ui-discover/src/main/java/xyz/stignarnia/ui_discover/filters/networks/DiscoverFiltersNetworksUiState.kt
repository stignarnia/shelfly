package xyz.stignarnia.ui_discover.filters.networks

import xyz.stignarnia.ui_model.Network

internal data class DiscoverFiltersNetworksUiState(
  val networks: List<Network>? = null,
  val isLoading: Boolean? = null,
)
