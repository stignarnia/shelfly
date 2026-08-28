package xyz.stignarnia.uiProgressMovies.progress.recycler

import androidx.annotation.StringRes
import xyz.stignarnia.uiBase.common.MovieListItem
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiModel.SpoilersSettings
import xyz.stignarnia.uiModel.Translation
import java.time.format.DateTimeFormatter

sealed class ProgressMovieListItem(
  override val movie: Movie,
  override val image: Image,
  override val isLoading: Boolean = false,
) : MovieListItem {
  data class MovieItem(
    override val movie: Movie,
    override val image: Image,
    override val isLoading: Boolean = false,
    val isPinned: Boolean,
    val translation: Translation? = null,
    val dateFormat: DateTimeFormatter? = null,
    val sortOrder: SortOrder? = null,
    val userRating: Int? = null,
    val spoilers: SpoilersSettings,
  ) : ProgressMovieListItem(movie, image, isLoading)

  data class HeaderItem(
    override val movie: Movie,
    override val image: Image,
    override val isLoading: Boolean = false,
    @get:StringRes val textResId: Int,
  ) : ProgressMovieListItem(movie, image, isLoading) {
    companion object {
      fun create(
        @StringRes textResId: Int,
      ) = HeaderItem(
        movie = Movie.EMPTY,
        image = Image.createUnavailable(ImageType.POSTER),
        textResId = textResId,
      )
    }

    override fun isSameAs(other: MovieListItem) = textResId == (other as? HeaderItem)?.textResId
  }

  data class FiltersItem(
    val sortOrder: SortOrder,
    val sortType: SortType,
  ) : ProgressMovieListItem(
      movie = Movie.EMPTY,
      image = Image.createUnknown(ImageType.POSTER),
      isLoading = false,
    )
}
