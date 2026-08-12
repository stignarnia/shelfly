package xyz.stignarnia.ui_progress.main

import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_model.CalendarMode

data class ProgressMainUiState(
  val timestamp: Long? = null,
  val searchQuery: String? = null,
  val calendarMode: CalendarMode? = null,
  val resetScroll: Event<Boolean>? = null,
)
