package xyz.stignarnia.data_remote.tmdb.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TmdbTranslation(
  // ex: zh
  val iso_639_1: String,
  // ex: CN
  val iso_3166_1: String,
  val data: Data?,
) {

  @JsonClass(generateAdapter = true)
  data class Data(
    val biography: String?,
  )
}
