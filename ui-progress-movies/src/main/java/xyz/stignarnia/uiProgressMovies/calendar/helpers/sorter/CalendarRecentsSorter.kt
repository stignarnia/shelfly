package xyz.stignarnia.uiProgressMovies.calendar.helpers.sorter

import xyz.stignarnia.uiModel.Movie
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRecentsSorter
  @Inject
  constructor() : CalendarSorter {
    override fun sort() = compareByDescending<Movie> { it.released }.thenByDescending { it.year }
  }
