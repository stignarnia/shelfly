package xyz.stignarnia.ui_show.quicksetup

import xyz.stignarnia.ui_model.Episode
import xyz.stignarnia.ui_model.Season

data class QuickSetupListItem(
  val episode: Episode,
  val season: Season,
  val isHeader: Boolean = false,
  val isChecked: Boolean = false,
)
