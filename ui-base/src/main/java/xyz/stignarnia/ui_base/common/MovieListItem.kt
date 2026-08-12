package xyz.stignarnia.ui_base.common

import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.Movie

interface MovieListItem {
  val movie: Movie
  val image: Image
  val isLoading: Boolean

  infix fun isSameAs(other: MovieListItem) = movie.ids.tmdb == other.movie.ids.tmdb
}
