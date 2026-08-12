package xyz.stignarnia.ui_my_movies.watchlist.recycler

import xyz.stignarnia.ui_base.common.MovieListItem
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.Translation
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
