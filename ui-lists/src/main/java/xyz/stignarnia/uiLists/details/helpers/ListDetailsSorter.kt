package xyz.stignarnia.uiLists.details.helpers

import xyz.stignarnia.uiLists.details.recycler.ListDetailsItem
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortOrder.DATE_ADDED
import xyz.stignarnia.uiModel.SortOrder.NAME
import xyz.stignarnia.uiModel.SortOrder.NEWEST
import xyz.stignarnia.uiModel.SortOrder.RANDOM
import xyz.stignarnia.uiModel.SortOrder.RANK
import xyz.stignarnia.uiModel.SortOrder.RATING
import xyz.stignarnia.uiModel.SortOrder.USER_RATING
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiModel.SortType.ASCENDING
import xyz.stignarnia.uiModel.SortType.DESCENDING
import java.util.UUID
import javax.inject.Inject

class ListDetailsSorter
  @Inject
  constructor() {
    fun sort(
      sortOrder: SortOrder,
      sortType: SortType,
    ) = when (sortType) {
      ASCENDING -> sortAscending(sortOrder)
      DESCENDING -> sortDescending(sortOrder)
    }

    private fun sortAscending(sortOrder: SortOrder): Comparator<ListDetailsItem> =
      when (sortOrder) {
        RANK -> {
          compareBy { it.rank }
        }

        NAME -> {
          compareBy { getTitle(it) }
        }

        NEWEST -> {
          compareBy<ListDetailsItem> { it.getYear() }.thenBy { it.getDate() }
        }

        RATING -> {
          compareBy { it.getRating() }
        }

        USER_RATING -> {
          compareByDescending<ListDetailsItem> { it.userRating != null }
            .thenBy { it.userRating }
            .thenBy { getTitle(it) }
        }

        DATE_ADDED -> {
          compareBy { it.listedAt }
        }

        RANDOM -> {
          compareBy { UUID.randomUUID() }
        }

        else -> {
          throw IllegalStateException("Invalid sort order")
        }
      }

    private fun sortDescending(sortOrder: SortOrder): Comparator<ListDetailsItem> =
      when (sortOrder) {
        RANK -> {
          compareByDescending { it.rank }
        }

        NAME -> {
          compareByDescending { getTitle(it) }
        }

        NEWEST -> {
          compareByDescending<ListDetailsItem> { it.getYear() }.thenByDescending { it.getDate() }
        }

        RATING -> {
          compareByDescending { it.getRating() }
        }

        USER_RATING -> {
          compareByDescending<ListDetailsItem> { it.userRating != null }
            .thenByDescending { it.userRating }
            .thenBy { getTitle(it) }
        }

        DATE_ADDED -> {
          compareByDescending { it.listedAt }
        }

        RANDOM -> {
          compareBy { UUID.randomUUID() }
        }

        else -> {
          throw IllegalStateException("Invalid sort order")
        }
      }

    private fun getTitle(item: ListDetailsItem): String =
      if (item.translation?.hasTitle == true) {
        item.translation.title
      } else {
        item.getTitleNoThe().uppercase()
      }
  }
