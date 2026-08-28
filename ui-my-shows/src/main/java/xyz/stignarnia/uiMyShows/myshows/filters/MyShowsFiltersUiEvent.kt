
package xyz.stignarnia.uiMyShows.myshows.filters

import xyz.stignarnia.uiBase.utilities.events.Event

internal sealed class MyShowsFiltersUiEvent<T>(
  action: T,
) : Event<T>(action) {
  object ApplyFilters : MyShowsFiltersUiEvent<Unit>(Unit)

  object CloseFilters : MyShowsFiltersUiEvent<Unit>(Unit)
}
