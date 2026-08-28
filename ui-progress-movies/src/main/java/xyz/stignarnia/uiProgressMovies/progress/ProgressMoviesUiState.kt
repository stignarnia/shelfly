package xyz.stignarnia.uiProgressMovies.progress

import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiProgressMovies.progress.recycler.ProgressMovieListItem

data class ProgressMoviesUiState(
  val items: List<ProgressMovieListItem>? = null,
  val scrollReset: Event<Boolean>? = null,
  val sortOrder: Event<Pair<SortOrder, SortType>>? = null,
  val isOverScrollEnabled: Boolean = false,
)
