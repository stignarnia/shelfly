package xyz.stignarnia.ui_show.sections.seasons.helpers

import xyz.stignarnia.ui_show.sections.seasons.recycler.SeasonListItem

data class SeasonsBundle(
  val seasons: List<SeasonListItem>?,
  val isLocal: Boolean,
)
