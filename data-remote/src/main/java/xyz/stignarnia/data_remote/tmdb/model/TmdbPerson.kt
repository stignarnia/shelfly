package xyz.stignarnia.data_remote.tmdb.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TmdbPerson(
  val id: Long,
  val name: String?,
  val place_of_birth: String?,
  val homepage: String?,
  val character: String?,
  val department: String?,
  val roles: List<Role>?,
  val jobs: List<Job>?,
  val job: String?,
  val deathday: String?,
  val birthday: String?,
  val biography: String?,
  val imdb_id: String?,
  val known_for_department: String?,
  val profile_path: String?,
  val total_episode_count: Int?,
) {

  @JsonClass(generateAdapter = true)
  data class Role(
    val character: String?,
  )

  @JsonClass(generateAdapter = true)
  data class Job(
    val job: String?,
  )

  enum class Type {
    CAST,
    CREW,
  }
}
