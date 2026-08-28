package xyz.stignarnia.uiProgressMovies.calendar

import xyz.stignarnia.uiModel.CalendarMode
import xyz.stignarnia.uiProgressMovies.calendar.recycler.CalendarMovieListItem

data class CalendarMoviesUiState(
  val items: List<CalendarMovieListItem>? = null,
  val mode: CalendarMode = CalendarMode.PRESENT_FUTURE,
)
