package xyz.stignarnia.uiDiscover

import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiDiscover.recycler.DiscoverListItem
import xyz.stignarnia.uiModel.DiscoverFilters

data class DiscoverUiState(
  val items: List<DiscoverListItem>? = null,
  val isLoading: Boolean? = null,
  var filters: DiscoverFilters? = null,
  var resetScroll: Event<Boolean>? = null,
)
