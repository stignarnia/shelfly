package xyz.stignarnia.uiMyMovies.mymovies.helpers

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
import xyz.stignarnia.uiMyMovies.mymovies.recycler.MyMoviesItem
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyMoviesSorter
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

        RATING -> {
          compareBy { it.movie.rating }
        }

        USER_RATING -> {
          compareByDescending<MyMoviesItem> { it.userRating != null }
            .thenBy { it.userRating }
            .thenBy { getTitle(it) }
        }

        DATE_ADDED -> {
          compareBy { it.movie.updatedAt }
        }

        RUNTIME -> {
          compareBy { it.movie.runtime }
        }

        NEWEST -> {
          compareBy<MyMoviesItem> { it.movie.year }.thenBy { it.movie.released }
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

        RATING -> {
          compareByDescending { it.movie.rating }
        }

        USER_RATING -> {
          compareByDescending<MyMoviesItem> { it.userRating != null }
            .thenByDescending { it.userRating }
            .thenBy { getTitle(it) }
        }

        DATE_ADDED -> {
          compareByDescending { it.movie.updatedAt }
        }

        RUNTIME -> {
          compareByDescending { it.movie.runtime }
        }

        NEWEST -> {
          compareByDescending<MyMoviesItem> { it.movie.year }.thenByDescending { it.movie.released }
        }

        RANDOM -> {
          compareBy { UUID.randomUUID() }
        }

        else -> {
          throw IllegalStateException("Invalid sort order")
        }
      }

    private fun getTitle(item: MyMoviesItem): String {
      val translatedTitle =
        if (item.translation?.hasTitle == true) {
          item.translation.title
        } else {
          item.movie.titleNoThe
        }
      return translatedTitle.uppercase()
    }
  }
