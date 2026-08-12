package xyz.stignarnia.ui_progress_movies.helpers

import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortOrder.DATE_ADDED
import xyz.stignarnia.ui_model.SortOrder.NAME
import xyz.stignarnia.ui_model.SortOrder.NEWEST
import xyz.stignarnia.ui_model.SortOrder.RANDOM
import xyz.stignarnia.ui_model.SortOrder.RATING
import xyz.stignarnia.ui_model.SortOrder.RUNTIME
import xyz.stignarnia.ui_model.SortOrder.USER_RATING
import xyz.stignarnia.ui_model.SortType
import xyz.stignarnia.ui_model.SortType.ASCENDING
import xyz.stignarnia.ui_model.SortType.DESCENDING
import xyz.stignarnia.ui_progress_movies.progress.recycler.ProgressMovieListItem
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressMoviesItemsSorter @Inject constructor() {

  fun sort(
    sortOrder: SortOrder,
    sortType: SortType,
  ) = when (sortType) {
    ASCENDING -> sortAscending(sortOrder)
    DESCENDING -> sortDescending(sortOrder)
  }

  private fun sortAscending(sortOrder: SortOrder) =
    when (sortOrder) {
      NAME -> {
        compareBy { getTitle(it) }
      }
      RUNTIME -> {
        compareBy { it.movie.runtime }
      }
      RATING -> {
        compareBy { it.movie.rating }
      }
      USER_RATING -> {
        compareByDescending<ProgressMovieListItem.MovieItem> { it.userRating != null }
          .thenBy { it.userRating }
          .thenBy { getTitle(it) }
      }
      DATE_ADDED -> {
        compareBy { it.movie.updatedAt }
      }
      NEWEST -> {
        compareBy<ProgressMovieListItem.MovieItem> { it.movie.released }
          .thenBy { it.movie.year }
      }
      RANDOM -> {
        compareBy { UUID.randomUUID() }
      }
      else -> {
        throw IllegalStateException("Invalid sort order")
      }
    }

  private fun sortDescending(sortOrder: SortOrder) =
    when (sortOrder) {
      NAME -> {
        compareByDescending { getTitle(it) }
      }
      RUNTIME -> {
        compareByDescending { it.movie.runtime }
      }
      RATING -> {
        compareByDescending { it.movie.rating }
      }
      USER_RATING -> {
        compareByDescending<ProgressMovieListItem.MovieItem> { it.userRating != null }
          .thenByDescending { it.userRating }
          .thenBy { getTitle(it) }
      }
      DATE_ADDED -> {
        compareByDescending { it.movie.updatedAt }
      }
      NEWEST -> {
        compareByDescending<ProgressMovieListItem.MovieItem> { it.movie.released }
          .thenByDescending { it.movie.year }
      }
      RANDOM -> {
        compareBy { UUID.randomUUID() }
      }
      else -> {
        throw IllegalStateException("Invalid sort order")
      }
    }

  private fun getTitle(item: ProgressMovieListItem.MovieItem): String {
    val translatedTitle =
      if (item.translation?.hasTitle == true) {
        item.translation.title
      } else {
        item.movie.titleNoThe
      }
    return translatedTitle.uppercase()
  }
}
