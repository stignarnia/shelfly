
package xyz.stignarnia.uiDiscoverMovies.filters.genres

import xyz.stignarnia.uiBase.utilities.events.Event

internal sealed class DiscoverMoviesFiltersGenresUiEvent<T>(
  action: T,
) : Event<T>(action) {
  object ApplyFilters : DiscoverMoviesFiltersGenresUiEvent<Unit>(Unit)

  object CloseFilters : DiscoverMoviesFiltersGenresUiEvent<Unit>(Unit)
}
