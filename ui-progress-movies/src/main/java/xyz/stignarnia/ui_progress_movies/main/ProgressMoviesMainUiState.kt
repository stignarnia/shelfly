package xyz.stignarnia.ui_progress_movies.main

import xyz.stignarnia.ui_model.CalendarMode

data class ProgressMoviesMainUiState(
  val timestamp: Long? = null,
  val searchQuery: String? = null,
  val calendarMode: CalendarMode? = null,
)
