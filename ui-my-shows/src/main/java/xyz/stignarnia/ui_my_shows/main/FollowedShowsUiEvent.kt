package xyz.stignarnia.ui_my_shows.main

import xyz.stignarnia.ui_base.utilities.events.Event

sealed class FollowedShowsUiEvent<T>(
  action: T,
) : Event<T>(action)
