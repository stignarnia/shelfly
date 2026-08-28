package xyz.stignarnia.uiMovie.sections.related.recycler

import xyz.stignarnia.uiBase.common.MovieListItem
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.Movie

data class RelatedListItem(
  override val movie: Movie,
  override val image: Image,
  override var isLoading: Boolean = false,
  val isFollowed: Boolean = false,
  val isWatchlist: Boolean = false,
) : MovieListItem
