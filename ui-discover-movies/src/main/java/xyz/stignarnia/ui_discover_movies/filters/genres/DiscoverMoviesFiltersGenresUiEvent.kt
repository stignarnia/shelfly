@file:Suppress("ktlint:standard:filename")

package xyz.stignarnia.ui_discover_movies.filters.genres

import xyz.stignarnia.ui_base.utilities.events.Event

internal sealed class DiscoverMoviesFiltersGenresUiEvent<T>(
  action: T,
) : Event<T>(action) {
  object ApplyFilters : DiscoverMoviesFiltersGenresUiEvent<Unit>(Unit)

  object CloseFilters : DiscoverMoviesFiltersGenresUiEvent<Unit>(Unit)
}
