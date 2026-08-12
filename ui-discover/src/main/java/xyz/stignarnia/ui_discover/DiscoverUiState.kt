package xyz.stignarnia.ui_discover

import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_discover.recycler.DiscoverListItem
import xyz.stignarnia.ui_model.DiscoverFilters

data class DiscoverUiState(
  val items: List<DiscoverListItem>? = null,
  val isLoading: Boolean? = null,
  var filters: DiscoverFilters? = null,
  var resetScroll: Event<Boolean>? = null,
)
