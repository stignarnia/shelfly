package xyz.stignarnia.uiShow.sections.nextepisode.helpers

import xyz.stignarnia.uiModel.Episode
import xyz.stignarnia.uiModel.Show
import java.time.format.DateTimeFormatter

data class NextEpisodeBundle(
  val nextEpisode: Pair<Show, Episode>,
  val isWatched: Boolean,
  val dateFormat: DateTimeFormatter? = null,
)
