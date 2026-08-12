@file:Suppress("ktlint:standard:filename")

package xyz.stignarnia.ui_progress_movies.main

import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.ProgressDateSelectionType

data class MovieCheckActionUiEvent(
  val movie: Movie,
  val dateSelectionType: ProgressDateSelectionType,
) : Event<Movie>(movie)

object RequestWidgetsUpdate : Event<Unit>(Unit)
