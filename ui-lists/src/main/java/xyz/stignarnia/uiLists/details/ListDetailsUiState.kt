package xyz.stignarnia.uiLists.details

import xyz.stignarnia.uiBase.common.ListViewMode
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiLists.details.recycler.ListDetailsItem
import xyz.stignarnia.uiModel.CustomList

data class ListDetailsUiState(
  val listDetails: CustomList? = null,
  val listItems: List<ListDetailsItem>? = null,
  val resetScroll: Event<Boolean>? = null,
  val deleteEvent: Event<Boolean>? = null,
  val isFiltersVisible: Boolean = false,
  val isManageMode: Boolean = false,
  val isQuickRemoveEnabled: Boolean = false,
  val isLoading: Boolean = false,
  val viewMode: ListViewMode = ListViewMode.LIST_NORMAL,
)
