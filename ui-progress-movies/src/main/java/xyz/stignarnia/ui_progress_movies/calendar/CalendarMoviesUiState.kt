package xyz.stignarnia.ui_progress_movies.calendar

import xyz.stignarnia.ui_model.CalendarMode
import xyz.stignarnia.ui_progress_movies.calendar.recycler.CalendarMovieListItem

data class CalendarMoviesUiState(
  val items: List<CalendarMovieListItem>? = null,
  val mode: CalendarMode = CalendarMode.PRESENT_FUTURE,
)
