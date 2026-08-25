package xyz.stignarnia.data_remote.tmdb.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TmdbImages(
  val backdrops: List<TmdbImage>?,
  val posters: List<TmdbImage>?,
  val stills: List<TmdbImage>?,
  val profiles: List<TmdbImage>?,
) {

  companion object {
    val EMPTY = TmdbImages(emptyList(), emptyList(), emptyList(), emptyList())
  }
}
