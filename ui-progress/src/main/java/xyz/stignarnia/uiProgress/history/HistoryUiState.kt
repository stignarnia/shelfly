package xyz.stignarnia.uiProgress.history

import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiProgress.history.entities.HistoryListItem

internal data class HistoryUiState(
  val items: List<HistoryListItem> = emptyList(),
  val isLoading: Boolean = false,
  val resetScrollEvent: Event<Boolean>? = null,
)
