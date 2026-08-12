package xyz.stignarnia.ui_movie.sections.collections.details

import xyz.stignarnia.ui_movie.sections.collections.details.recycler.MovieDetailsCollectionItem

data class MovieDetailsCollectionUiState(
  val items: List<MovieDetailsCollectionItem>? = null,
)
