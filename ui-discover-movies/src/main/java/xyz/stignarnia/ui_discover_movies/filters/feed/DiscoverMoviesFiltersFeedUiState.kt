package xyz.stignarnia.ui_discover_movies.filters.feed

import xyz.stignarnia.ui_model.DiscoverFeed

internal data class DiscoverMoviesFiltersFeedUiState(
  val feedOrder: DiscoverFeed? = null,
  val isLoading: Boolean? = null,
)
