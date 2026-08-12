package xyz.stignarnia.ui_lists.lists.helpers

import xyz.stignarnia.ui_model.CustomList
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortOrder.DATE_UPDATED
import xyz.stignarnia.ui_model.SortOrder.NAME
import xyz.stignarnia.ui_model.SortOrder.NEWEST
import xyz.stignarnia.ui_model.SortType
import xyz.stignarnia.ui_model.SortType.ASCENDING
import xyz.stignarnia.ui_model.SortType.DESCENDING
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListsSorter @Inject constructor() {

  fun sort(
    sortOrder: SortOrder,
    sortType: SortType,
  ) = when (sortType) {
    ASCENDING -> sortAscending(sortOrder)
    DESCENDING -> sortDescending(sortOrder)
  }

  private fun sortAscending(sortOrder: SortOrder): Comparator<CustomList> =
    when (sortOrder) {
      NAME -> compareBy { it.name }
      NEWEST -> compareBy { it.createdAt }
      DATE_UPDATED -> compareBy { it.updatedAt }
      else -> throw IllegalStateException("Invalid sort order")
    }

  private fun sortDescending(sortOrder: SortOrder): Comparator<CustomList> =
    when (sortOrder) {
      NAME -> compareByDescending { it.name }
      NEWEST -> compareByDescending { it.createdAt }
      DATE_UPDATED -> compareByDescending { it.updatedAt }
      else -> throw IllegalStateException("Invalid sort order")
    }
}
