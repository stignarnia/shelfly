@file:Suppress("ktlint:standard:filename")

package xyz.stignarnia.ui_my_movies.filters

import xyz.stignarnia.ui_base.utilities.events.Event

internal sealed class CollectionFiltersUiEvent<T>(
  action: T,
) : Event<T>(action) {
  object ApplyFilters : CollectionFiltersUiEvent<Unit>(Unit)

  object CloseFilters : CollectionFiltersUiEvent<Unit>(Unit)
}
