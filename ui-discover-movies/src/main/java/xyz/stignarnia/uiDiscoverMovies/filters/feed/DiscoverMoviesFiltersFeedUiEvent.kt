
package xyz.stignarnia.uiDiscoverMovies.filters.feed

import xyz.stignarnia.uiBase.utilities.events.Event

internal sealed class DiscoverMoviesFiltersFeedUiEvent<T>(
  action: T,
) : Event<T>(action) {
  object ApplyFilters : DiscoverMoviesFiltersFeedUiEvent<Unit>(Unit)

  object CloseFilters : DiscoverMoviesFiltersFeedUiEvent<Unit>(Unit)
}
