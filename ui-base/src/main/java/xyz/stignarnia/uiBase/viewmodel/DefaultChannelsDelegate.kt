package xyz.stignarnia.uiBase.viewmodel

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiBase.utilities.events.MessageEvent

class DefaultChannelsDelegate : ChannelsDelegate {
  override val messageChannel = Channel<MessageEvent>(Channel.BUFFERED)
  override val messageFlow = messageChannel.receiveAsFlow()

  override val eventChannel = Channel<Event<*>>(Channel.BUFFERED)
  override val eventFlow = eventChannel.receiveAsFlow()
}
