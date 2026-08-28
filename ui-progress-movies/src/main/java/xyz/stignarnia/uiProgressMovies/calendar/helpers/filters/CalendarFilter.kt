package xyz.stignarnia.uiProgressMovies.calendar.helpers.filters

import xyz.stignarnia.uiModel.Movie
import java.time.ZonedDateTime

interface CalendarFilter {
  fun filter(
    now: ZonedDateTime,
    movie: Movie,
  ): Boolean
}
