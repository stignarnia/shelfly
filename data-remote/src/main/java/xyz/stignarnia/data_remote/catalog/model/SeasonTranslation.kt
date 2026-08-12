package xyz.stignarnia.data_remote.catalog.model

data class SeasonTranslation(
  val season: Int,
  val number: Int,
  val ids: Ids,
  val translations: List<Translation>?,
)
