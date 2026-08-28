package xyz.stignarnia.uiBase.common

import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.Movie

interface MovieListItem {
  val movie: Movie
  val image: Image
  val isLoading: Boolean

  infix fun isSameAs(other: MovieListItem) = movie.ids.tmdb == other.movie.ids.tmdb
}
