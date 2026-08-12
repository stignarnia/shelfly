package xyz.stignarnia.ui_progress.progress

import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortType
import xyz.stignarnia.ui_progress.progress.recycler.ProgressListItem

data class ProgressUiState(
  val items: List<ProgressListItem>? = null,
  val isLoading: Boolean = false,
  val isOverScrollEnabled: Boolean = false,
  val scrollReset: Event<Boolean>? = null,
  val sortOrder: Event<Triple<SortOrder, SortType, Boolean>>? = null,
)
