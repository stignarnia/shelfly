package xyz.stignarnia.uiSearch

import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiModel.RecentSearch
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiSearch.recycler.SearchListItem
import xyz.stignarnia.uiSearch.utilities.SearchOptions

data class SearchUiState(
  val searchItems: List<SearchListItem>? = null,
  val searchItemsAnimate: Event<Boolean>? = null,
  val recentSearchItems: List<RecentSearch>? = null,
  val suggestionsItems: List<SearchListItem>? = null,
  val searchOptions: SearchOptions? = null,
  val sortOrder: Event<Pair<SortOrder, SortType>>? = null,
  val isSearching: Boolean = false,
  val isEmpty: Boolean = false,
  val isInitial: Boolean = false,
  val isFiltersVisible: Boolean = false,
  val isMoviesEnabled: Boolean = false,
  val resetScroll: Event<Boolean>? = null,
)
