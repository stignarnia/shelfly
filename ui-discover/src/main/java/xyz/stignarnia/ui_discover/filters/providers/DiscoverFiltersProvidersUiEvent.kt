@file:Suppress("ktlint:standard:filename")

package xyz.stignarnia.ui_discover.filters.providers

import xyz.stignarnia.ui_base.utilities.events.Event

internal sealed class DiscoverFiltersProvidersUiEvent<T>(
  action: T,
) : Event<T>(action) {
  object ApplyFilters : DiscoverFiltersProvidersUiEvent<Unit>(Unit)

  object CloseFilters : DiscoverFiltersProvidersUiEvent<Unit>(Unit)
}
