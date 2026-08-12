package xyz.stignarnia.ui_progress_movies.calendar.helpers.groupers

import xyz.stignarnia.ui_progress_movies.calendar.recycler.CalendarMovieListItem
import java.time.ZonedDateTime

interface CalendarGrouper {
  fun groupByTime(
    nowUtc: ZonedDateTime,
    items: List<CalendarMovieListItem.MovieItem>,
  ): List<CalendarMovieListItem>
}
