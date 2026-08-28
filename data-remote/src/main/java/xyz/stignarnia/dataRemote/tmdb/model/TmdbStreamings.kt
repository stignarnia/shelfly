package xyz.stignarnia.dataRemote.tmdb.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TmdbStreamings(
  val id: Long,
  val results: Map<String, TmdbStreamingCountry>,
)
