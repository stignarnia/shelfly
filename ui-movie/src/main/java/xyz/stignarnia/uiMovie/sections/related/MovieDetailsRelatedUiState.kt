package xyz.stignarnia.uiMovie.sections.related

import xyz.stignarnia.uiMovie.sections.related.recycler.RelatedListItem

data class MovieDetailsRelatedUiState(
  val isLoading: Boolean = true,
  val relatedMovies: List<RelatedListItem>? = null,
)
