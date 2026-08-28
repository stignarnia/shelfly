package xyz.stignarnia.uiMyShows.main

import xyz.stignarnia.uiBase.utilities.events.Event

sealed class FollowedShowsUiEvent<T>(
  action: T,
) : Event<T>(action)
