package xyz.stignarnia.dataRemote.tmdb.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TmdbStreamingCountry(
  val link: String,
  val flatrate: List<TmdbStreamingService>?,
  val free: List<TmdbStreamingService>?,
  val buy: List<TmdbStreamingService>?,
  val rent: List<TmdbStreamingService>?,
  val ads: List<TmdbStreamingService>?,
)
