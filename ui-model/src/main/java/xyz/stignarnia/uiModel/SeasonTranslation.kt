package xyz.stignarnia.uiModel

data class SeasonTranslation(
  val ids: Ids,
  val title: String,
  val seasonNumber: Int,
  val episodeNumber: Int,
  val overview: String,
  val language: String,
  val isLocal: Boolean = false,
)
