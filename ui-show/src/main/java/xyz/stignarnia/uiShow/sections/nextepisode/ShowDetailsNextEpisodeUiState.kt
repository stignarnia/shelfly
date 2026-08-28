package xyz.stignarnia.uiShow.sections.nextepisode

import xyz.stignarnia.uiModel.SpoilersSettings
import xyz.stignarnia.uiShow.sections.nextepisode.helpers.NextEpisodeBundle

data class ShowDetailsNextEpisodeUiState(
  val nextEpisode: NextEpisodeBundle? = null,
  val spoilersSettings: SpoilersSettings? = null,
)
