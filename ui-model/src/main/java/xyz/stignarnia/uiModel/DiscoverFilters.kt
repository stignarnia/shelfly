package xyz.stignarnia.uiModel

data class DiscoverFilters(
  val feedOrder: DiscoverFeed = DiscoverFeed.TRENDING,
  val hideAnticipated: Boolean = true,
  val hideCollection: Boolean = false,
  val genres: List<Genre> = emptyList(),
  val providers: List<StreamingProvider> = emptyList(),
)
