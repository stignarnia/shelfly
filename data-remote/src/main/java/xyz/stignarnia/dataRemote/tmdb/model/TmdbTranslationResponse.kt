package xyz.stignarnia.dataRemote.tmdb.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TmdbTranslationResponse(
  val id: Long?,
  val translations: List<TmdbTranslation>?,
)
