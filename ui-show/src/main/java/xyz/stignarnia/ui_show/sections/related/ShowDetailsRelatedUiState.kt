package xyz.stignarnia.ui_show.sections.related

import xyz.stignarnia.ui_show.sections.related.recycler.RelatedListItem

data class ShowDetailsRelatedUiState(
  val isLoading: Boolean = true,
  val relatedShows: List<RelatedListItem>? = null,
)
