package xyz.stignarnia.ui_my_shows.myshows

import xyz.stignarnia.ui_base.common.ListViewMode
import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_my_shows.myshows.recycler.MyShowsItem

data class MyShowsUiState(
  val items: List<MyShowsItem>? = null,
  val showEmptyView: Boolean = false,
  val viewMode: ListViewMode = ListViewMode.LIST_NORMAL,
  val resetScrollMap: Event<List<MyShowsItem.Type>?>? = null,
)
