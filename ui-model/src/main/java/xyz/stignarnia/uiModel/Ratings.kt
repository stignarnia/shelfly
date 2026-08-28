package xyz.stignarnia.uiModel

data class Ratings(
  val tmdb: Value? = null,
  val imdb: Value? = null,
  val metascore: Value? = null,
  val rottenTomatoes: Value? = null,
  val rottenTomatoesUrl: String? = null,
  val isHidden: Boolean = false,
  val isTapToReveal: Boolean = false,
  /**
   * IMDb, Metascore and Rotten Tomatoes all come from OMDb.
   * With no key configured they can never be filled in, so the strip marks them rather than showing an empty slot.
   */
  val isOmdbKeyMissing: Boolean = false,
) {
  fun isAnyLoading() =
    tmdb?.isLoading == true ||
      imdb?.isLoading == true ||
      metascore?.isLoading == true ||
      rottenTomatoes?.isLoading == true

  data class Value(
    val value: String?,
    val isLoading: Boolean,
  )
}
