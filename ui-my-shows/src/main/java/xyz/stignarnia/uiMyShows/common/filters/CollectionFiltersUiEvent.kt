
package xyz.stignarnia.uiMyShows.common.filters

import xyz.stignarnia.uiBase.utilities.events.Event

internal sealed class CollectionFiltersUiEvent<T>(
  action: T,
) : Event<T>(action) {
  object ApplyFilters : CollectionFiltersUiEvent<Unit>(Unit)

  object CloseFilters : CollectionFiltersUiEvent<Unit>(Unit)
}
