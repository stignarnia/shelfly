@file:Suppress("ktlint:standard:filename")

package xyz.stignarnia.ui_my_shows.myshows.filters

import xyz.stignarnia.ui_base.utilities.events.Event

internal sealed class MyShowsFiltersUiEvent<T>(
  action: T,
) : Event<T>(action) {
  object ApplyFilters : MyShowsFiltersUiEvent<Unit>(Unit)

  object CloseFilters : MyShowsFiltersUiEvent<Unit>(Unit)
}
