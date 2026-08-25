package xyz.stignarnia.data_remote.tmdb.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TmdbPeople(
  val id: Long,
  val cast: List<TmdbPerson>?,
  val crew: List<TmdbPerson>?,
)
