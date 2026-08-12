package xyz.stignarnia.ui_movie.sections.related.recycler

import xyz.stignarnia.ui_base.common.MovieListItem
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.Movie

data class RelatedListItem(
  override val movie: Movie,
  override val image: Image,
  override var isLoading: Boolean = false,
  val isFollowed: Boolean = false,
  val isWatchlist: Boolean = false,
) : MovieListItem
