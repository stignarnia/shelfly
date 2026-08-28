package xyz.stignarnia.uiProgress.main

import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiModel.CalendarMode

data class ProgressMainUiState(
  val timestamp: Long? = null,
  val searchQuery: String? = null,
  val calendarMode: CalendarMode? = null,
  val resetScroll: Event<Boolean>? = null,
)
