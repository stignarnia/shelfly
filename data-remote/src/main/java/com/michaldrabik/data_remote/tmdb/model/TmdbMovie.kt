package com.michaldrabik.data_remote.tmdb.model

/**
 * A movie as returned by /movie/{id}. List endpoints return the same shape with
 * most fields absent, so everything is nullable.
 */
data class TmdbMovie(
  val id: Long?,
  val title: String?,
  val overview: String?,
  val release_date: String?,
  val runtime: Int?,
  val homepage: String?,
  val status: String?,
  val vote_average: Float?,
  val vote_count: Long?,
  val original_language: String?,
  val genres: List<TmdbGenre>?,
  val genre_ids: List<Int>?,
  val production_countries: List<TmdbCountry>?,
  val belongs_to_collection: TmdbCollectionRef?,
  val external_ids: TmdbExternalIds?,
  val release_dates: TmdbReleaseDates?,
  val videos: TmdbVideos?,
)

data class TmdbCountry(
  val iso_3166_1: String?,
  val name: String?,
)

/**
 * Age certifications per country, from
 * /movie/{id}?append_to_response=release_dates. Shows use content_ratings
 * instead, with a different shape.
 */
data class TmdbReleaseDates(
  val results: List<Result>?,
) {

  data class Result(
    val iso_3166_1: String?,
    val release_dates: List<ReleaseDate>?,
  )

  data class ReleaseDate(
    val certification: String?,
  )
}

data class TmdbCollectionRef(
  val id: Long?,
  val name: String?,
)

/**
 * A franchise from /collection/{id}. TMDB places a movie in at most one.
 */
data class TmdbCollection(
  val id: Long?,
  val name: String?,
  val overview: String?,
  val parts: List<TmdbMovie>?,
)
