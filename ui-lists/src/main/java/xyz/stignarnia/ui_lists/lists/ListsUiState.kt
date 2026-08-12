package xyz.stignarnia.ui_lists.lists

import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_lists.lists.recycler.ListsItem
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortType

data class ListsUiState(
  val items: List<ListsItem>? = null,
  val resetScroll: Event<Boolean> = Event(false),
  val sortOrder: Pair<SortOrder, SortType>? = null,
)
