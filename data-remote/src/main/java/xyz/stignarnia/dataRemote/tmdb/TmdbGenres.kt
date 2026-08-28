package xyz.stignarnia.dataRemote.tmdb

/**
 * Maps the app's genre slugs onto TMDB genre ids, which are what /discover filters on.
 * TMDB keeps separate genre lists for shows and movies, and they do not line up: shows collapse action and adventure into one genre, and science fiction into "Sci-Fi & Fantasy".
 *
 * Slugs with no TMDB equivalent map to nothing and are dropped from the filter rather than silently returning everything.
 */
object TmdbGenres {
  private val SHOW_GENRES =
    mapOf(
      "action" to 10759,
      "adventure" to 10759,
      "animation" to 16,
      "anime" to 16,
      "comedy" to 35,
      "crime" to 80,
      "documentary" to 99,
      "drama" to 18,
      "fantasy" to 10765,
      "science-fiction" to 10765,
      "war" to 10768,
      "western" to 37,
      "mystery" to 9648,
      "family" to 10751,
    )

  private val MOVIE_GENRES =
    mapOf(
      "action" to 28,
      "adventure" to 12,
      "animation" to 16,
      "anime" to 16,
      "comedy" to 35,
      "crime" to 80,
      "documentary" to 99,
      "drama" to 18,
      "fantasy" to 14,
      "history" to 36,
      "horror" to 27,
      "science-fiction" to 878,
      "romance" to 10749,
      "thriller" to 53,
      "war" to 10752,
      "western" to 37,
      "mystery" to 9648,
      "family" to 10751,
    )

  /**
   * List endpoints return genre ids instead of full genre objects, so ids have to be resolved back to slugs.
   * Several TMDB genres cover two of the app's slugs; the first match wins.
   */
  fun showSlugs(ids: List<Int>): List<String> = slugs(ids, SHOW_GENRES)

  fun movieSlugs(ids: List<Int>): List<String> = slugs(ids, MOVIE_GENRES)

  private fun slugs(
    ids: List<Int>,
    genres: Map<String, Int>,
  ): List<String> = ids.mapNotNull { id -> genres.entries.firstOrNull { it.value == id }?.key }

  /**
   * TMDB treats a comma as AND and a pipe as OR.
   * The app's filters are "any of these", so entries are joined with a pipe.
   */
  fun showQuery(slugs: List<String>): String? = query(slugs, SHOW_GENRES)

  fun movieQuery(slugs: List<String>): String? = query(slugs, MOVIE_GENRES)

  private fun query(
    slugs: List<String>,
    genres: Map<String, Int>,
  ): String? {
    val ids = slugs.mapNotNull { genres[it.lowercase()] }.distinct()
    return if (ids.isEmpty()) null else ids.joinToString("|")
  }
}
