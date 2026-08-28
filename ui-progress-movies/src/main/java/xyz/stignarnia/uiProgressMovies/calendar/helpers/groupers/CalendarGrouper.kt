package xyz.stignarnia.uiProgressMovies.calendar.helpers.groupers

import xyz.stignarnia.uiProgressMovies.calendar.recycler.CalendarMovieListItem
import java.time.ZonedDateTime

interface CalendarGrouper {
  fun groupByTime(
    nowUtc: ZonedDateTime,
    items: List<CalendarMovieListItem.MovieItem>,
  ): List<CalendarMovieListItem>
}
