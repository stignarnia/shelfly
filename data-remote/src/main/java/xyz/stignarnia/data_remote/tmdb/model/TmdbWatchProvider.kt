package xyz.stignarnia.data_remote.tmdb.model

import com.squareup.moshi.JsonClass

/**
 * An entry of the region-wide provider directory, which is a different payload from the per-title availability in [TmdbStreamingService]: it carries no offer type and its priority is published per region as well as globally.
 */
@JsonClass(generateAdapter = true)
data class TmdbWatchProvider(
  val provider_id: Long,
  val provider_name: String,
  val logo_path: String?,
  val display_priority: Long?,
  val display_priorities: Map<String, Long>?,
)
