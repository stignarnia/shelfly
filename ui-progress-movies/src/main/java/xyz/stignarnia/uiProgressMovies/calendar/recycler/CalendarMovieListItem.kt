package xyz.stignarnia.uiProgressMovies.calendar.recycler

import androidx.annotation.StringRes
import xyz.stignarnia.uiBase.common.MovieListItem
import xyz.stignarnia.uiModel.CalendarMode
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.SpoilersSettings
import xyz.stignarnia.uiModel.Translation
import java.time.format.DateTimeFormatter

sealed class CalendarMovieListItem(
  override val movie: Movie,
  override val image: Image,
  override val isLoading: Boolean = false,
) : MovieListItem {
  data class MovieItem(
    override val movie: Movie,
    override val image: Image,
    override val isLoading: Boolean = false,
    val isWatched: Boolean,
    val isWatchlist: Boolean,
    val translation: Translation? = null,
    val dateFormat: DateTimeFormatter? = null,
    val spoilers: SpoilersSettings,
  ) : CalendarMovieListItem(movie, image, isLoading)

  data class Header(
    @get:StringRes val textResId: Int,
    val calendarMode: CalendarMode,
  ) : CalendarMovieListItem(
      movie = Movie.EMPTY,
      image = Image.createUnknown(ImageType.POSTER),
      isLoading = false,
    ) {
    companion object {
      fun create(
        @StringRes textResId: Int,
        mode: CalendarMode,
      ) = Header(
        textResId = textResId,
        calendarMode = mode,
      )
    }

    override fun isSameAs(other: MovieListItem) = textResId == (other as? Header)?.textResId
  }

  data class Filters(
    val mode: CalendarMode,
  ) : CalendarMovieListItem(
      movie = Movie.EMPTY,
      image = Image.createUnknown(ImageType.POSTER),
      isLoading = false,
    )
}
