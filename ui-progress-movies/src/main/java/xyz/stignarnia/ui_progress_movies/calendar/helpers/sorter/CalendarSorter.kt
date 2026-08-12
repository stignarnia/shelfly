package xyz.stignarnia.ui_progress_movies.calendar.helpers.sorter

import xyz.stignarnia.ui_model.Movie
import java.util.Comparator

interface CalendarSorter {
  fun sort(): Comparator<Movie>
}
