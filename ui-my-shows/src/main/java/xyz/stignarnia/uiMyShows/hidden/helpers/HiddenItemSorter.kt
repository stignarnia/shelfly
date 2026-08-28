package xyz.stignarnia.uiMyShows.hidden.helpers

import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortOrder.DATE_ADDED
import xyz.stignarnia.uiModel.SortOrder.NAME
import xyz.stignarnia.uiModel.SortOrder.NEWEST
import xyz.stignarnia.uiModel.SortOrder.RATING
import xyz.stignarnia.uiModel.SortOrder.RECENTLY_WATCHED
import xyz.stignarnia.uiModel.SortOrder.USER_RATING
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiModel.SortType.ASCENDING
import xyz.stignarnia.uiModel.SortType.DESCENDING
import xyz.stignarnia.uiMyShows.common.recycler.CollectionListItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HiddenItemSorter
  @Inject
  constructor() {
    fun sort(
      sortOrder: SortOrder,
      sortType: SortType,
    ) = when (sortType) {
      ASCENDING -> sortAscending(sortOrder)
      DESCENDING -> sortDescending(sortOrder)
    }

    private fun sortAscending(sortOrder: SortOrder): Comparator<CollectionListItem.ShowItem> =
      when (sortOrder) {
        NAME -> {
          compareBy { getTitle(it) }
        }

        RATING -> {
          compareBy { it.show.rating }
        }

        USER_RATING -> {
          compareByDescending<CollectionListItem.ShowItem> { it.userRating != null }
            .thenBy { it.userRating }
            .thenBy { getTitle(it) }
        }

        DATE_ADDED -> {
          compareBy { it.show.createdAt }
        }

        RECENTLY_WATCHED -> {
          compareBy { it.show.updatedAt }
        }

        NEWEST -> {
          compareBy<CollectionListItem.ShowItem> { it.show.year }.thenBy { it.show.firstAired }
        }

        else -> {
          throw IllegalStateException("Invalid sort order")
        }
      }

    private fun sortDescending(sortOrder: SortOrder): Comparator<CollectionListItem.ShowItem> =
      when (sortOrder) {
        NAME -> {
          compareByDescending { getTitle(it) }
        }

        RATING -> {
          compareByDescending { it.show.rating }
        }

        USER_RATING -> {
          compareByDescending<CollectionListItem.ShowItem> { it.userRating != null }
            .thenByDescending { it.userRating }
            .thenBy { getTitle(it) }
        }

        DATE_ADDED -> {
          compareByDescending { it.show.createdAt }
        }

        RECENTLY_WATCHED -> {
          compareByDescending { it.show.updatedAt }
        }

        NEWEST -> {
          compareByDescending<CollectionListItem.ShowItem> { it.show.year }
            .thenByDescending { it.show.firstAired }
        }

        else -> {
          throw IllegalStateException("Invalid sort order")
        }
      }

    private fun getTitle(item: CollectionListItem.ShowItem): String {
      val translatedTitle =
        if (item.translation?.hasTitle == true) {
          item.translation.title
        } else {
          item.show.titleNoThe
        }
      return translatedTitle.uppercase()
    }
  }
