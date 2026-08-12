package xyz.stignarnia.ui_lists.manage.recycler

import xyz.stignarnia.ui_model.CustomList

data class ManageListsItem(
  val list: CustomList,
  val isChecked: Boolean,
  val isEnabled: Boolean,
)
