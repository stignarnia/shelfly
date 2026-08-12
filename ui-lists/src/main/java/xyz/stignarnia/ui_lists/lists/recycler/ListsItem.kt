package xyz.stignarnia.ui_lists.lists.recycler

import xyz.stignarnia.ui_lists.lists.helpers.ListsItemImage
import xyz.stignarnia.ui_model.CustomList
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortType
import java.time.format.DateTimeFormatter

data class ListsItem(
  val list: CustomList,
  val images: List<ListsItemImage>,
  val sortOrder: Pair<SortOrder, SortType>,
  val dateFormat: DateTimeFormatter? = null,
)
