package xyz.stignarnia.ui_discover.filters.feed

import xyz.stignarnia.ui_model.DiscoverFeed

internal data class DiscoverFiltersFeedUiState(
  val feedOrder: DiscoverFeed? = null,
  val isLoading: Boolean? = null,
)
