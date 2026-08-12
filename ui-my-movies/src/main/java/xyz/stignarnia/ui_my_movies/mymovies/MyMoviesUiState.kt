package xyz.stignarnia.ui_my_movies.mymovies

import xyz.stignarnia.ui_base.common.ListViewMode
import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_my_movies.mymovies.recycler.MyMoviesItem

data class MyMoviesUiState(
  val items: List<MyMoviesItem>? = null,
  val showEmptyView: Boolean = false,
  val viewMode: ListViewMode = ListViewMode.LIST_NORMAL,
  val resetScroll: Event<Boolean>? = null,
)
