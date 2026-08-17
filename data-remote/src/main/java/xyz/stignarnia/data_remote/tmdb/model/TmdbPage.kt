package xyz.stignarnia.data_remote.tmdb.model

/**
 * TMDB paginates every list endpoint at 20 items per page, so callers that want a longer list have to walk pages.
 */
data class TmdbPage<T>(
  val page: Int?,
  val total_pages: Int?,
  val total_results: Int?,
  val results: List<T>?,
)

/**
 * An entry from /search/multi, which mixes shows, movies and people.
 * Show and movie fields both appear here because the payload is a union - "media_type" says which ones are populated.
 */
data class TmdbSearchItem(
  val id: Long?,
  val media_type: String?,
  val name: String?,
  val title: String?,
  val overview: String?,
  val first_air_date: String?,
  val release_date: String?,
  val vote_average: Float?,
  val vote_count: Long?,
  val genre_ids: List<Int>?,
  val origin_country: List<String>?,
) {

  companion object {
    const val MEDIA_TYPE_SHOW = "tv"
    const val MEDIA_TYPE_MOVIE = "movie"
  }

  fun isShow() = media_type == MEDIA_TYPE_SHOW

  fun isMovie() = media_type == MEDIA_TYPE_MOVIE
}

/**
 * Everything a person has appeared in or worked on, from /person/{id}/combined_credits.
 * Entries carry "media_type" like search results.
 */
data class TmdbPersonCredits(
  val cast: List<TmdbSearchItem>?,
  val crew: List<TmdbSearchItem>?,
)

/**
 * Result of /find/{external_id}, used to resolve an IMDb id into a TMDB one.
 */
data class TmdbFindResult(
  val tv_results: List<TmdbShow>?,
  val movie_results: List<TmdbMovie>?,
)
