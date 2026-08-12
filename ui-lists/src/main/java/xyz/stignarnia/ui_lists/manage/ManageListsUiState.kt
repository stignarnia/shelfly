package xyz.stignarnia.ui_lists.manage

import xyz.stignarnia.ui_lists.manage.recycler.ManageListsItem

data class ManageListsUiState(
  val items: List<ManageListsItem>? = null,
)
