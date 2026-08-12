package xyz.stignarnia.ui_show.sections.nextepisode

import xyz.stignarnia.ui_model.SpoilersSettings
import xyz.stignarnia.ui_show.sections.nextepisode.helpers.NextEpisodeBundle

data class ShowDetailsNextEpisodeUiState(
  val nextEpisode: NextEpisodeBundle? = null,
  val spoilersSettings: SpoilersSettings? = null,
)
