package xyz.stignarnia.ui_my_movies.main

import xyz.stignarnia.ui_base.utilities.events.Event

sealed class FollowedMoviesUiEvent<T>(
  action: T,
) : Event<T>(action)
