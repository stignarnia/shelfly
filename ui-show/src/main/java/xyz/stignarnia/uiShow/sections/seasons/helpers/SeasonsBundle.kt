package xyz.stignarnia.uiShow.sections.seasons.helpers

import xyz.stignarnia.uiShow.sections.seasons.recycler.SeasonListItem

data class SeasonsBundle(
  val seasons: List<SeasonListItem>?,
  val isLocal: Boolean,
)
