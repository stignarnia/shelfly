@file:Suppress("ktlint:standard:filename")

package xyz.stignarnia.ui_lists.details

import xyz.stignarnia.ui_base.utilities.events.Event

sealed class ListDetailsUiEvent<T>(
  action: T,
) : Event<T>(action)
