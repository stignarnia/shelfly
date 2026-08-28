package xyz.stignarnia.uiSearch.utilities

import xyz.stignarnia.common.Mode
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType

data class SearchOptions(
  val filters: List<Mode> = emptyList(),
  val sortOrder: SortOrder = SortOrder.RANK,
  val sortType: SortType = SortType.ASCENDING,
)
