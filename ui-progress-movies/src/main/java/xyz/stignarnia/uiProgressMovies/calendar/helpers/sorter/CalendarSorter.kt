package xyz.stignarnia.uiProgressMovies.calendar.helpers.sorter

import xyz.stignarnia.uiModel.Movie
import java.util.Comparator

interface CalendarSorter {
  fun sort(): Comparator<Movie>
}
