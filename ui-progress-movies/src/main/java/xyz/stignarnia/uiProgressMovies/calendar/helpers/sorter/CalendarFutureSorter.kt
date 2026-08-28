package xyz.stignarnia.uiProgressMovies.calendar.helpers.sorter

import xyz.stignarnia.uiModel.Movie
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarFutureSorter
  @Inject
  constructor() : CalendarSorter {
    override fun sort() = compareBy<Movie> { it.released }.thenBy { it.year }
  }
