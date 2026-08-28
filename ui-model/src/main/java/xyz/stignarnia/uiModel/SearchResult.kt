package xyz.stignarnia.uiModel

data class SearchResult(
  val order: Int,
  val show: Show,
  val movie: Movie,
) {
  val isShow = show != Show.EMPTY

  val tmdbId = if (show != Show.EMPTY) show.tmdbId else movie.tmdbId
}
