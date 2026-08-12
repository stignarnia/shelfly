@file:Suppress("ktlint:standard:filename")

package xyz.stignarnia.ui_discover.filters.genres

import xyz.stignarnia.ui_base.utilities.events.Event

internal sealed class DiscoverFiltersGenresUiEvent<T>(
  action: T,
) : Event<T>(action) {
  object ApplyFilters : DiscoverFiltersGenresUiEvent<Unit>(Unit)

  object CloseFilters : DiscoverFiltersGenresUiEvent<Unit>(Unit)
}
