package xyz.stignarnia.uiProgress.progress

import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiProgress.progress.recycler.ProgressListItem

data class ProgressUiState(
  val items: List<ProgressListItem>? = null,
  val isLoading: Boolean = false,
  val isOverScrollEnabled: Boolean = false,
  val scrollReset: Event<Boolean>? = null,
  val sortOrder: Event<Triple<SortOrder, SortType, Boolean>>? = null,
)
