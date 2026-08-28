
package xyz.stignarnia.uiDiscoverMovies.filters.providers

import xyz.stignarnia.uiBase.utilities.events.Event

internal sealed class DiscoverMoviesFiltersProvidersUiEvent<T>(
  action: T,
) : Event<T>(action) {
  object ApplyFilters : DiscoverMoviesFiltersProvidersUiEvent<Unit>(Unit)

  object CloseFilters : DiscoverMoviesFiltersProvidersUiEvent<Unit>(Unit)
}
