package xyz.stignarnia.uiMyMovies.mymovies

import xyz.stignarnia.uiBase.common.ListViewMode
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiMyMovies.mymovies.recycler.MyMoviesItem

data class MyMoviesUiState(
  val items: List<MyMoviesItem>? = null,
  val showEmptyView: Boolean = false,
  val viewMode: ListViewMode = ListViewMode.LIST_NORMAL,
  val resetScroll: Event<Boolean>? = null,
)
