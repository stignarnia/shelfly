package xyz.stignarnia.data_remote.tmdb.model

import com.squareup.moshi.JsonClass

/**
 * A TV show as returned by /tv/{id}.
 * List endpoints return the same shape with most fields absent, so everything is nullable.
 */
@JsonClass(generateAdapter = true)
data class TmdbShow(
  val id: Long?,
  val name: String?,
  val overview: String?,
  val first_air_date: String?,
  val last_air_date: String?,
  val episode_run_time: List<Int>?,
  val homepage: String?,
  val status: String?,
  val vote_average: Float?,
  val vote_count: Long?,
  val number_of_episodes: Int?,
  val genres: List<TmdbGenre>?,
  val genre_ids: List<Int>?,
  val networks: List<TmdbNetwork>?,
  val origin_country: List<String>?,
  val seasons: List<TmdbSeason>?,
  val next_episode_to_air: TmdbEpisode?,
  val external_ids: TmdbExternalIds?,
  val content_ratings: TmdbContentRatings?,
  val videos: TmdbVideos?,
)

@JsonClass(generateAdapter = true)
data class TmdbSeason(
  val id: Long?,
  val season_number: Int?,
  val name: String?,
  val overview: String?,
  val air_date: String?,
  val episode_count: Int?,
  val vote_average: Float?,
  val episodes: List<TmdbEpisode>?,
)

@JsonClass(generateAdapter = true)
data class TmdbEpisode(
  val id: Long?,
  val season_number: Int?,
  val episode_number: Int?,
  val name: String?,
  val overview: String?,
  val air_date: String?,
  val runtime: Int?,
  val vote_average: Float?,
  val vote_count: Int?,
)

@JsonClass(generateAdapter = true)
data class TmdbGenre(
  val id: Int?,
  val name: String?,
)

@JsonClass(generateAdapter = true)
data class TmdbNetwork(
  val id: Long?,
  val name: String?,
  val logo_path: String? = null,
)

@JsonClass(generateAdapter = true)
data class TmdbExternalIds(
  val imdb_id: String?,
  val tvdb_id: Long?,
)

/**
 * Age certifications per country, from /tv/{id}?append_to_response=content_ratings.
 */
@JsonClass(generateAdapter = true)
data class TmdbContentRatings(
  val results: List<Result>?,
) {

  @JsonClass(generateAdapter = true)
  data class Result(
    val iso_3166_1: String?,
    val rating: String?,
  )
}

@JsonClass(generateAdapter = true)
data class TmdbVideos(
  val results: List<TmdbVideo>?,
)

@JsonClass(generateAdapter = true)
data class TmdbVideo(
  val key: String?,
  val site: String?,
  val type: String?,
)
