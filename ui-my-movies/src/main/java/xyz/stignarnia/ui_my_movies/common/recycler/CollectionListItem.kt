package xyz.stignarnia.ui_my_movies.common.recycler

import xyz.stignarnia.ui_base.common.MovieListItem
import xyz.stignarnia.ui_model.Genre
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.ImageType
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortType
import xyz.stignarnia.ui_model.Translation
import xyz.stignarnia.ui_model.UpcomingFilter
import java.time.format.DateTimeFormatter

sealed class CollectionListItem(
  override val movie: Movie,
  override val image: Image,
  override val isLoading: Boolean = false,
) : MovieListItem {

  data class MovieItem(
    override val movie: Movie,
    override val image: Image,
    override val isLoading: Boolean = false,
    val dateFormat: DateTimeFormatter,
    val fullDateFormat: DateTimeFormatter,
    val translation: Translation? = null,
    val userRating: Int? = null,
    val sortOrder: SortOrder? = null,
    val spoilers: Spoilers,
  ) : CollectionListItem(
      movie = movie,
      image = image,
      isLoading = isLoading,
    ) {

    data class Spoilers(
      val isSpoilerHidden: Boolean,
      val isSpoilerRatingsHidden: Boolean,
      val isSpoilerTapToReveal: Boolean,
    )
  }

  data class FiltersItem(
    val sortOrder: SortOrder,
    val sortType: SortType,
    val upcoming: UpcomingFilter,
    val genres: List<Genre>,
    val count: Int,
  ) : CollectionListItem(
      movie = Movie.EMPTY,
      image = Image.createUnknown(ImageType.POSTER),
      isLoading = false,
    ) {

    fun hasActiveFilters() = upcoming.isActive() || genres.isNotEmpty()
  }
}
