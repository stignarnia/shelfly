package xyz.stignarnia.data_remote.tmdb.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TmdbWatchProviders(
  val results: List<TmdbWatchProvider>?,
)
