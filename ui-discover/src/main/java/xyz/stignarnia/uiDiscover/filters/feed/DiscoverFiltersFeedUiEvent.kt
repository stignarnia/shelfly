
package xyz.stignarnia.uiDiscover.filters.feed

import xyz.stignarnia.uiBase.utilities.events.Event

internal sealed class DiscoverFiltersFeedUiEvent<T>(
  action: T,
) : Event<T>(action) {
  object ApplyFilters : DiscoverFiltersFeedUiEvent<Unit>(Unit)

  object CloseFilters : DiscoverFiltersFeedUiEvent<Unit>(Unit)
}
