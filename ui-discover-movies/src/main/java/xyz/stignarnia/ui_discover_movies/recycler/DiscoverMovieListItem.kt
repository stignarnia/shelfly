package xyz.stignarnia.ui_discover_movies.recycler

import xyz.stignarnia.ui_base.common.MovieListItem
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.Translation

data class DiscoverMovieListItem(
  override val movie: Movie,
  override val image: Image,
  override var isLoading: Boolean = false,
  val isCollected: Boolean = false,
  val isWatchlist: Boolean = false,
  val translation: Translation? = null,
) : MovieListItem
