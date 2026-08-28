package xyz.stignarnia.uiModel

import androidx.annotation.StringRes

enum class MyMoviesSection(
  @param:StringRes val displayString: Int,
) {
  RECENTS(
    displayString = R.string.textHeaderRecentlyAdded,
  ),
  ALL(
    displayString = R.string.textHeaderAll,
  ),
}
