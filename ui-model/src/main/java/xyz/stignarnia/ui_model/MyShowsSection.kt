package xyz.stignarnia.ui_model

import androidx.annotation.StringRes
import xyz.stignarnia.ui_model.ShowStatus.CANCELED
import xyz.stignarnia.ui_model.ShowStatus.ENDED
import xyz.stignarnia.ui_model.ShowStatus.IN_PRODUCTION
import xyz.stignarnia.ui_model.ShowStatus.PLANNED
import xyz.stignarnia.ui_model.ShowStatus.RETURNING

enum class MyShowsSection(
  @param:StringRes val displayString: Int,
  val allowedStatuses: List<ShowStatus> = emptyList(),
) {
  RECENTS(
    displayString = R.string.textHeaderRecentlyAdded,
  ),
  WATCHING(
    allowedStatuses = listOf(RETURNING),
    displayString = R.string.textHeaderWatching,
  ),
  FINISHED(
    allowedStatuses = listOf(CANCELED, ENDED),
    displayString = R.string.textHeaderFinished,
  ),
  UPCOMING(
    allowedStatuses = listOf(IN_PRODUCTION, PLANNED, ShowStatus.UPCOMING),
    displayString = R.string.textHeaderReturning,
  ),
  ALL(
    displayString = R.string.textHeaderAll,
  ),
}
