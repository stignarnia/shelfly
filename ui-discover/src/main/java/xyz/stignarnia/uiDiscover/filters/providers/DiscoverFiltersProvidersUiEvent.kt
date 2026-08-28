
package xyz.stignarnia.uiDiscover.filters.providers

import xyz.stignarnia.uiBase.utilities.events.Event

internal sealed class DiscoverFiltersProvidersUiEvent<T>(
  action: T,
) : Event<T>(action) {
  object ApplyFilters : DiscoverFiltersProvidersUiEvent<Unit>(Unit)

  object CloseFilters : DiscoverFiltersProvidersUiEvent<Unit>(Unit)
}
