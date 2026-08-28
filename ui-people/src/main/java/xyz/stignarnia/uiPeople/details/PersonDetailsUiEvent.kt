
package xyz.stignarnia.uiPeople.details

import xyz.stignarnia.uiBase.utilities.events.Event

internal sealed class PersonDetailsUiEvent<T>(
  action: T,
) : Event<T>(action) {
  data class ScrollToPosition(
    val position: Int,
    val isSheetExpanded: Boolean,
    val isUpButtonVisible: Boolean,
  ) : PersonDetailsUiEvent<Int>(position)
}
