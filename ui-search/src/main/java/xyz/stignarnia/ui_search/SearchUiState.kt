package xyz.stignarnia.ui_search

import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_model.RecentSearch
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortType
import xyz.stignarnia.ui_search.recycler.SearchListItem
import xyz.stignarnia.ui_search.utilities.SearchOptions

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
