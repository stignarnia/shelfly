package xyz.stignarnia.uiShow.quicksetup

import xyz.stignarnia.uiModel.Episode
import xyz.stignarnia.uiModel.Season

data class QuickSetupListItem(
  val episode: Episode,
  val season: Season,
  val isHeader: Boolean = false,
  val isChecked: Boolean = false,
)
