@file:Suppress("ktlint:standard:filename")

package xyz.stignarnia.ui_discover_movies.filters.providers

import xyz.stignarnia.ui_base.utilities.events.Event

internal sealed class DiscoverMoviesFiltersProvidersUiEvent<T>(
  action: T,
) : Event<T>(action) {
  object ApplyFilters : DiscoverMoviesFiltersProvidersUiEvent<Unit>(Unit)

  object CloseFilters : DiscoverMoviesFiltersProvidersUiEvent<Unit>(Unit)
}
