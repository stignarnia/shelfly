package xyz.stignarnia.uiLists.manage

import xyz.stignarnia.uiLists.manage.recycler.ManageListsItem

data class ManageListsUiState(
  val items: List<ManageListsItem>? = null,
)
