package xyz.stignarnia.uiProgress.progress

import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType

/**
 * The chips above the Progress list.
 * Kept out of the list so they can float in the header and hide with it, the same as Discover's.
 */
data class ProgressFilters(
  val sortOrder: SortOrder,
  val sortType: SortType,
  val isUpcoming: Boolean,
  val isUpcomingEnabled: Boolean,
  val isOnHold: Boolean,
  val newAtTop: Boolean,
) {
  fun hasActiveFilters() = isUpcoming || isOnHold
}
