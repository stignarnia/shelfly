
package xyz.stignarnia.uiDiscover.filters.genres

import xyz.stignarnia.uiBase.utilities.events.Event

internal sealed class DiscoverFiltersGenresUiEvent<T>(
  action: T,
) : Event<T>(action) {
  object ApplyFilters : DiscoverFiltersGenresUiEvent<Unit>(Unit)

  object CloseFilters : DiscoverFiltersGenresUiEvent<Unit>(Unit)
}
