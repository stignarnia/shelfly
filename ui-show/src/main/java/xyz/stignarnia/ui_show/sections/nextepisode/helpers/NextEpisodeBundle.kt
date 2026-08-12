package xyz.stignarnia.ui_show.sections.nextepisode.helpers

import xyz.stignarnia.ui_model.Episode
import xyz.stignarnia.ui_model.Show
import java.time.format.DateTimeFormatter

data class NextEpisodeBundle(
  val nextEpisode: Pair<Show, Episode>,
  val isWatched: Boolean,
  val dateFormat: DateTimeFormatter? = null,
)
