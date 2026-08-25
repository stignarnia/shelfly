package xyz.stignarnia.data_remote.tmdb.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TmdbTranslationResponse(
  val id: Long?,
  val translations: List<TmdbTranslation>?,
)
