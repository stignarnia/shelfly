package xyz.stignarnia.uiLists.manage.recycler

import xyz.stignarnia.uiModel.CustomList

data class ManageListsItem(
  val list: CustomList,
  val isChecked: Boolean,
  val isEnabled: Boolean,
)
