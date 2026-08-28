package xyz.stignarnia.uiLists.lists.recycler

import xyz.stignarnia.uiLists.lists.helpers.ListsItemImage
import xyz.stignarnia.uiModel.CustomList
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import java.time.format.DateTimeFormatter

data class ListsItem(
  val list: CustomList,
  val images: List<ListsItemImage>,
  val sortOrder: Pair<SortOrder, SortType>,
  val dateFormat: DateTimeFormatter? = null,
)
