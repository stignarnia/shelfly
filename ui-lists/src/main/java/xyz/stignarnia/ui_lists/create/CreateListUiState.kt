package xyz.stignarnia.ui_lists.create

import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_model.CustomList

data class CreateListUiState(
  val listDetails: CustomList? = null,
  val isLoading: Boolean? = null,
  val onListUpdated: Event<CustomList>? = null,
)
