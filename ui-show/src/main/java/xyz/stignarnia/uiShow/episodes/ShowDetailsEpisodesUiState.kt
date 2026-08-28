package xyz.stignarnia.uiShow.episodes

import xyz.stignarnia.uiShow.episodes.recycler.EpisodeListItem
import xyz.stignarnia.uiShow.sections.seasons.recycler.SeasonListItem

data class ShowDetailsEpisodesUiState(
  val season: SeasonListItem? = null,
  val episodes: List<EpisodeListItem>? = null,
  val isInitialLoad: Boolean? = null,
)
