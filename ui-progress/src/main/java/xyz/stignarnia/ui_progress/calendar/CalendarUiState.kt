package xyz.stignarnia.ui_progress.calendar

import xyz.stignarnia.ui_model.CalendarMode
import xyz.stignarnia.ui_progress.calendar.recycler.CalendarListItem

data class CalendarUiState(
  val items: List<CalendarListItem>? = null,
  val mode: CalendarMode = CalendarMode.PRESENT_FUTURE,
)
