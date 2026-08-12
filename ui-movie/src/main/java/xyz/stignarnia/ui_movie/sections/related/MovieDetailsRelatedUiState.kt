package xyz.stignarnia.ui_movie.sections.related

import xyz.stignarnia.ui_movie.sections.related.recycler.RelatedListItem

data class MovieDetailsRelatedUiState(
  val isLoading: Boolean = true,
  val relatedMovies: List<RelatedListItem>? = null,
)
