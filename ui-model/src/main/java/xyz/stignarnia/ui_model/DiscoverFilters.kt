package xyz.stignarnia.ui_model

data class DiscoverFilters(
  val feedOrder: DiscoverFeed = DiscoverFeed.TRENDING,
  val hideAnticipated: Boolean = true,
  val hideCollection: Boolean = false,
  val genres: List<Genre> = emptyList(),
  val networks: List<Network> = emptyList(),
)
