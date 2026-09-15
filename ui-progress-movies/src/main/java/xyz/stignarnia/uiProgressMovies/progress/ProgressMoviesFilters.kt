package xyz.stignarnia.uiProgressMovies.progress

import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType

/**
 * The chips above the movies Progress list.
 * Kept out of the list so they can float in the header and hide with it, the same as Discover's.
 */
data class ProgressMoviesFilters(
  val sortOrder: SortOrder,
  val sortType: SortType,
)
