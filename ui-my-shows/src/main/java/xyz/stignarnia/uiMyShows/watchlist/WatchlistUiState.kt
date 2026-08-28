package xyz.stignarnia.uiMyShows.watchlist

import xyz.stignarnia.uiBase.common.ListViewMode
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiMyShows.common.recycler.CollectionListItem

data class WatchlistUiState(
  val items: List<CollectionListItem> = emptyList(),
  val viewMode: ListViewMode = ListViewMode.LIST_NORMAL,
  val resetScroll: Event<Boolean>? = null,
  val sortOrder: Event<Pair<SortOrder, SortType>>? = null,
)
