package xyz.stignarnia.uiShow.sections.related

import xyz.stignarnia.uiShow.sections.related.recycler.RelatedListItem

data class ShowDetailsRelatedUiState(
  val isLoading: Boolean = true,
  val relatedShows: List<RelatedListItem>? = null,
)
