package xyz.stignarnia.uiBase.events

sealed class Event

object ReloadData : Event()

// Shows, Movies Sync

data class ShowsMoviesSyncComplete(
  val count: Int,
) : Event()
