package xyz.stignarnia.data_remote.tmdb.model

data class TmdbTranslationResponse(
  val id: Long?,
  val translations: List<TmdbTranslation>?,
)
