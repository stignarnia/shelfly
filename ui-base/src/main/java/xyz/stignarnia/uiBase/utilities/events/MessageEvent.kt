package xyz.stignarnia.uiBase.utilities.events

import androidx.annotation.StringRes

sealed class MessageEvent(
  val textResId: Int,
) : Event<Int>(textResId) {
  data class Info(
    @param:StringRes val textRestId: Int,
    val isIndefinite: Boolean = false,
  ) : MessageEvent(textRestId)

  data class Error(
    @param:StringRes val textRestId: Int,
  ) : MessageEvent(textRestId)
}
