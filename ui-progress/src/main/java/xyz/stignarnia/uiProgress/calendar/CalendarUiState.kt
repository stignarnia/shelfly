package xyz.stignarnia.uiProgress.calendar

import xyz.stignarnia.uiModel.CalendarMode
import xyz.stignarnia.uiProgress.calendar.recycler.CalendarListItem

data class CalendarUiState(
  val items: List<CalendarListItem>? = null,
  val mode: CalendarMode = CalendarMode.PRESENT_FUTURE,
)
