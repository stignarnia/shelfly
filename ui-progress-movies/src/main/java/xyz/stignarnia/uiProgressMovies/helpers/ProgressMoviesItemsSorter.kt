package xyz.stignarnia.uiProgressMovies.helpers

import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortOrder.DATE_ADDED
import xyz.stignarnia.uiModel.SortOrder.NAME
import xyz.stignarnia.uiModel.SortOrder.NEWEST
import xyz.stignarnia.uiModel.SortOrder.RANDOM
import xyz.stignarnia.uiModel.SortOrder.RATING
import xyz.stignarnia.uiModel.SortOrder.RUNTIME
import xyz.stignarnia.uiModel.SortOrder.USER_RATING
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiModel.SortType.ASCENDING
import xyz.stignarnia.uiModel.SortType.DESCENDING
import xyz.stignarnia.uiProgressMovies.progress.recycler.ProgressMovieListItem
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressMoviesItemsSorter
  @Inject
  constructor() {
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
