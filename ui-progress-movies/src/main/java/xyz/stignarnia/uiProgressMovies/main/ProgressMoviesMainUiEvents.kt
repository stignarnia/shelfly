
package xyz.stignarnia.uiProgressMovies.main

import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.ProgressDateSelectionType

data class MovieCheckActionUiEvent(
  val movie: Movie,
  val dateSelectionType: ProgressDateSelectionType,
) : Event<Movie>(movie)

object RequestWidgetsUpdate : Event<Unit>(Unit)
