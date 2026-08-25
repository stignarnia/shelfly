
package xyz.stignarnia.ui_discover.filters.feed

import xyz.stignarnia.ui_base.utilities.events.Event

internal sealed class DiscoverFiltersFeedUiEvent<T>(
  action: T,
) : Event<T>(action) {
  object ApplyFilters : DiscoverFiltersFeedUiEvent<Unit>(Unit)

  object CloseFilters : DiscoverFiltersFeedUiEvent<Unit>(Unit)
}
