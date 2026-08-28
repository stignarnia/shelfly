package xyz.stignarnia.uiModel

/**
 * A streaming service as TMDB knows it in one region.
 * The id is what discover filters on; the name and logo are carried along so a saved filter can still label itself without another round trip.
 */
data class StreamingProvider(
  val id: Long,
  val name: String,
  val logoPath: String,
)
