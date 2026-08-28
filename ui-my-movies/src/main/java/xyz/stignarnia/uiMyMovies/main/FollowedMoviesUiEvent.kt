package xyz.stignarnia.uiMyMovies.main

import xyz.stignarnia.uiBase.utilities.events.Event

sealed class FollowedMoviesUiEvent<T>(
  action: T,
) : Event<T>(action)
