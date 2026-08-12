package xyz.stignarnia.ui_search.utilities

import xyz.stignarnia.common.Mode
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortType

data class SearchOptions(
  val filters: List<Mode> = emptyList(),
  val sortOrder: SortOrder = SortOrder.RANK,
  val sortType: SortType = SortType.ASCENDING,
)
