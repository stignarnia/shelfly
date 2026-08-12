package xyz.stignarnia.ui_progress_movies.progress

import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortType
import xyz.stignarnia.ui_progress_movies.progress.recycler.ProgressMovieListItem

data class ProgressMoviesUiState(
  val items: List<ProgressMovieListItem>? = null,
  val scrollReset: Event<Boolean>? = null,
  val sortOrder: Event<Pair<SortOrder, SortType>>? = null,
  val isOverScrollEnabled: Boolean = false,
)
