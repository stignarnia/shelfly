package xyz.stignarnia.uiDiscoverMovies.recycler

import xyz.stignarnia.uiBase.common.MovieListItem
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.Translation

data class DiscoverMovieListItem(
  override val movie: Movie,
  override val image: Image,
  override var isLoading: Boolean = false,
  val isCollected: Boolean = false,
  val isWatchlist: Boolean = false,
  val translation: Translation? = null,
) : MovieListItem
