package xyz.stignarnia.uiMovie.sections.collections.details

import xyz.stignarnia.uiMovie.sections.collections.details.recycler.MovieDetailsCollectionItem

data class MovieDetailsCollectionUiState(
  val items: List<MovieDetailsCollectionItem>? = null,
)
