
package xyz.stignarnia.uiLists.details

import xyz.stignarnia.uiBase.utilities.events.Event

sealed class ListDetailsUiEvent<T>(
  action: T,
) : Event<T>(action)
