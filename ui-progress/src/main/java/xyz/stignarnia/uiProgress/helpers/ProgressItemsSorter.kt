package xyz.stignarnia.uiProgress.helpers

import xyz.stignarnia.common.extensions.toMillis
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortOrder.EPISODES_LEFT
import xyz.stignarnia.uiModel.SortOrder.NAME
import xyz.stignarnia.uiModel.SortOrder.NEWEST
import xyz.stignarnia.uiModel.SortOrder.RANDOM
import xyz.stignarnia.uiModel.SortOrder.RATING
import xyz.stignarnia.uiModel.SortOrder.RECENTLY_WATCHED
import xyz.stignarnia.uiModel.SortOrder.USER_RATING
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiModel.SortType.ASCENDING
import xyz.stignarnia.uiModel.SortType.DESCENDING
import xyz.stignarnia.uiProgress.progress.recycler.ProgressListItem
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressItemsSorter
  @Inject
  constructor() {
    fun sort(
      sortOrder: SortOrder,
      sortType: SortType,
    ) = when (sortType) {
      ASCENDING -> sortAscending(sortOrder)
      DESCENDING -> sortDescending(sortOrder)
    }

    private fun sortAscending(sortOrder: SortOrder) =
      when (sortOrder) {
        NAME -> {
          compareBy { getTitle(it) }
        }

        RECENTLY_WATCHED -> {
          compareBy { it.show.updatedAt }
        }

        NEWEST -> {
          compareBy { it.episode?.firstAired?.toMillis() }
        }

        RATING -> {
          compareBy { it.show.rating }
        }

        USER_RATING -> {
          compareByDescending<ProgressListItem.Episode> { it.userRating != null }
            .thenBy { it.userRating }
            .thenBy { getTitle(it) }
        }

        EPISODES_LEFT -> {
          compareBy<ProgressListItem.Episode> { it.totalCount - it.watchedCount }.thenBy { getTitle(it) }
        }

        RANDOM -> {
          compareBy { UUID.randomUUID() }
        }

        else -> {
          throw IllegalStateException("Invalid sort order")
        }
      }

    private fun sortDescending(sortOrder: SortOrder) =
      when (sortOrder) {
        NAME -> {
          compareByDescending { getTitle(it) }
        }

        RECENTLY_WATCHED -> {
          compareByDescending { it.show.updatedAt }
        }

        NEWEST -> {
          compareByDescending { it.episode?.firstAired?.toMillis() }
        }

        RATING -> {
          compareByDescending { it.show.rating }
        }

        USER_RATING -> {
          compareByDescending<ProgressListItem.Episode> { it.userRating != null }
            .thenByDescending { it.userRating }
            .thenBy { getTitle(it) }
        }

        EPISODES_LEFT -> {
          compareByDescending<ProgressListItem.Episode> {
            it.totalCount - it.watchedCount
          }.thenBy { getTitle(it) }
        }

        RANDOM -> {
          compareBy { UUID.randomUUID() }
        }

        else -> {
          throw IllegalStateException("Invalid sort order")
        }
      }

    private fun getTitle(item: ProgressListItem.Episode): String {
      val translatedTitle =
        if (item.translations?.show?.hasTitle == false) {
          null
        } else {
          item.translations?.show?.title
        }
      return (translatedTitle ?: item.show.titleNoThe).uppercase(Locale.ROOT)
    }
  }
