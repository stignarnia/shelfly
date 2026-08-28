package xyz.stignarnia.uiDiscoverMovies

import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiDiscoverMovies.recycler.DiscoverMovieListItem
import xyz.stignarnia.uiModel.DiscoverFilters

data class DiscoverMoviesUiState(
  val items: List<DiscoverMovieListItem>? = null,
  val isLoading: Boolean? = null,
  var filters: DiscoverFilters? = null,
  var resetScroll: Event<Boolean>? = null,
)
