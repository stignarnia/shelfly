package xyz.stignarnia.uiProgressMovies.main

import xyz.stignarnia.uiModel.CalendarMode

data class ProgressMoviesMainUiState(
  val timestamp: Long? = null,
  val searchQuery: String? = null,
  val calendarMode: CalendarMode? = null,
)
