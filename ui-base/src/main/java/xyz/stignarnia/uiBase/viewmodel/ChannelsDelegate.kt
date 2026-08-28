package xyz.stignarnia.uiBase.viewmodel

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiBase.utilities.events.MessageEvent

interface ChannelsDelegate {
  val messageFlow: Flow<MessageEvent>
  val messageChannel: Channel<MessageEvent>

  val eventFlow: Flow<Event<*>>
  val eventChannel: Channel<Event<*>>
}
