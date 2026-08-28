package xyz.stignarnia.uiMyShows.myshows

import xyz.stignarnia.uiBase.common.ListViewMode
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiMyShows.myshows.recycler.MyShowsItem

data class MyShowsUiState(
  val items: List<MyShowsItem>? = null,
  val showEmptyView: Boolean = false,
  val viewMode: ListViewMode = ListViewMode.LIST_NORMAL,
  val resetScrollMap: Event<List<MyShowsItem.Type>?>? = null,
)
