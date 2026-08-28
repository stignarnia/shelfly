package xyz.stignarnia.uiLists.create

import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiModel.CustomList

data class CreateListUiState(
  val listDetails: CustomList? = null,
  val isLoading: Boolean? = null,
  val onListUpdated: Event<CustomList>? = null,
)
