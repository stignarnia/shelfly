package xyz.stignarnia.ui_base.viewmodel

import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_base.utilities.events.MessageEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow

interface ChannelsDelegate {

  val messageFlow: Flow<MessageEvent>
  val messageChannel: Channel<MessageEvent>

  val eventFlow: Flow<Event<*>>
  val eventChannel: Channel<Event<*>>
}
