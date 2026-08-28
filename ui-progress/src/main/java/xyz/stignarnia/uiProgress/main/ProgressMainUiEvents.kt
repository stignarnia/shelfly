
package xyz.stignarnia.uiProgress.main

import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiModel.Episode
import xyz.stignarnia.uiModel.EpisodeBundle
import xyz.stignarnia.uiModel.ProgressDateSelectionType
import xyz.stignarnia.uiModel.Show

data class EpisodeCheckActionUiEvent(
  val episode: EpisodeBundle,
  val dateSelectionType: ProgressDateSelectionType,
) : Event<EpisodeBundle>(episode)

data class OpenEpisodeDetails(
  val show: Show,
  val episode: Episode,
  val isWatched: Boolean,
) : Event<Episode>(episode)

object RequestWidgetsUpdate : Event<Unit>(Unit)
