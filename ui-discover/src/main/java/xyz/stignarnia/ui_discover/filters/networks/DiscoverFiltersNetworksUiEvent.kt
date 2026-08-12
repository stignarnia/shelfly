@file:Suppress("ktlint:standard:filename")

package xyz.stignarnia.ui_discover.filters.networks

import xyz.stignarnia.ui_base.utilities.events.Event

internal sealed class DiscoverFiltersNetworksUiEvent<T>(
  action: T,
) : Event<T>(action) {
  object ApplyFilters : DiscoverFiltersNetworksUiEvent<Unit>(Unit)

  object CloseFilters : DiscoverFiltersNetworksUiEvent<Unit>(Unit)
}
