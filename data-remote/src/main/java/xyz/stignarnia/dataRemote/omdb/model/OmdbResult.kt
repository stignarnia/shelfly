package xyz.stignarnia.dataRemote.omdb.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OmdbResult(
  val Ratings: List<OmdbRating>?,
  val imdbRating: String?,
  val imdbVotes: String?,
  val Metascore: String?,
  val tomatoURL: String?,
)

@JsonClass(generateAdapter = true)
data class OmdbRating(
  val Source: String?,
  val Value: String?,
)
