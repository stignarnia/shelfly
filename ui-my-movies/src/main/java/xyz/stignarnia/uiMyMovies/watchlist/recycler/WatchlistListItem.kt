package xyz.stignarnia.uiMyMovies.watchlist.recycler

import xyz.stignarnia.uiBase.common.MovieListItem
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.Translation
import java.time.format.DateTimeFormatter

sealed class WatchlistListItem(
  override val movie: Movie,
  override val image: Image,
  override val isLoading: Boolean = false,
) : MovieListItem {
  data class MovieItem(
    override val movie: Movie,
    override val image: Image,
    override val isLoading: Boolean = false,
    val translation: Translation? = null,
    val userRating: Int? = null,
    val dateFormat: DateTimeFormatter? = null,
    val fullDateFormat: DateTimeFormatter? = null,
  ) : WatchlistListItem(
      movie = movie,
      image = image,
      isLoading = isLoading,
    )
}
