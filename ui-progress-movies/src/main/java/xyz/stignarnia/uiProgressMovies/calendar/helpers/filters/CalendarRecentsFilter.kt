package xyz.stignarnia.uiProgressMovies.calendar.helpers.filters

import xyz.stignarnia.uiModel.Movie
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRecentsFilter
  @Inject
  constructor() : CalendarFilter {
    override fun filter(
      now: ZonedDateTime,
      movie: Movie,
    ) = movie.released?.isBefore(now.toLocalDate()) == true
  }
