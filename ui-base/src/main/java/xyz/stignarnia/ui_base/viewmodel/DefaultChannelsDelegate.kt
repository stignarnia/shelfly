package xyz.stignarnia.ui_base.viewmodel

import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_base.utilities.events.MessageEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class DefaultChannelsDelegate : ChannelsDelegate {

  override val messageChannel = Channel<MessageEvent>(Channel.BUFFERED)
  override val messageFlow = messageChannel.receiveAsFlow()

  override val eventChannel = Channel<Event<*>>(Channel.BUFFERED)
  override val eventFlow = eventChannel.receiveAsFlow()
}
