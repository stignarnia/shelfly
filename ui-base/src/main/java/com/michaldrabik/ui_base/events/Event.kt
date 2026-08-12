package com.michaldrabik.ui_base.events

sealed class Event

object ReloadData : Event()

// Shows, Movies Sync

data class ShowsMoviesSyncComplete(
  val count: Int,
) : Event()
