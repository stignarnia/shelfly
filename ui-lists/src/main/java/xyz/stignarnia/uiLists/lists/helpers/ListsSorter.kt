package xyz.stignarnia.uiLists.lists.helpers

import xyz.stignarnia.uiModel.CustomList
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortOrder.DATE_UPDATED
import xyz.stignarnia.uiModel.SortOrder.NAME
import xyz.stignarnia.uiModel.SortOrder.NEWEST
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiModel.SortType.ASCENDING
import xyz.stignarnia.uiModel.SortType.DESCENDING
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListsSorter
  @Inject
  constructor() {
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
