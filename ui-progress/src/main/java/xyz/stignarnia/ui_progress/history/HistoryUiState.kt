package xyz.stignarnia.ui_progress.history

import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_progress.history.entities.HistoryListItem

internal data class HistoryUiState(
  val items: List<HistoryListItem> = emptyList(),
  val isLoading: Boolean = false,
  val resetScrollEvent: Event<Boolean>? = null,
)
