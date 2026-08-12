package xyz.stignarnia.ui_my_movies.hidden

import xyz.stignarnia.ui_base.common.ListViewMode
import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortType
import xyz.stignarnia.ui_my_movies.common.recycler.CollectionListItem

data class HiddenUiState(
  val items: List<CollectionListItem> = emptyList(),
  val viewMode: ListViewMode = ListViewMode.LIST_NORMAL,
  val resetScroll: Event<Boolean>? = null,
  val sortOrder: Event<Pair<SortOrder, SortType>>? = null,
)
