package xyz.stignarnia.dataRemote.tmdb.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TmdbStreamingService(
  val display_priority: Long,
  val logo_path: String,
  val provider_id: Long,
  val provider_name: String,
)
