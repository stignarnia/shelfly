package xyz.stignarnia.uiModel

import androidx.annotation.StringRes
import xyz.stignarnia.uiModel.ShowStatus.CANCELED
import xyz.stignarnia.uiModel.ShowStatus.ENDED
import xyz.stignarnia.uiModel.ShowStatus.IN_PRODUCTION
import xyz.stignarnia.uiModel.ShowStatus.PLANNED
import xyz.stignarnia.uiModel.ShowStatus.RETURNING

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
