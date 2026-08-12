@file:Suppress("ktlint:standard:filename")

package xyz.stignarnia.ui_progress.main

import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_model.Episode
import xyz.stignarnia.ui_model.EpisodeBundle
import xyz.stignarnia.ui_model.ProgressDateSelectionType
import xyz.stignarnia.ui_model.Show

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
