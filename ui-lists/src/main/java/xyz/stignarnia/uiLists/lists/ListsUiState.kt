package xyz.stignarnia.uiLists.lists

import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiLists.lists.recycler.ListsItem
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType

data class ListsUiState(
  val items: List<ListsItem>? = null,
  val resetScroll: Event<Boolean> = Event(false),
  val sortOrder: Pair<SortOrder, SortType>? = null,
)
