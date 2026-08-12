package xyz.stignarnia.ui_progress_movies.calendar.helpers.filters

import xyz.stignarnia.ui_model.Movie
import java.time.ZonedDateTime

interface CalendarFilter {
  fun filter(
    now: ZonedDateTime,
    movie: Movie,
  ): Boolean
}
