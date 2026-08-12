package xyz.stignarnia.ui_episodes.details

import xyz.stignarnia.ui_model.Episode
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.RatingState
import xyz.stignarnia.ui_model.SpoilersSettings
import xyz.stignarnia.ui_model.Translation
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class EpisodeDetailsUiState(
  val image: Image? = null,
  val isImageLoading: Boolean = false,
  val episodes: List<Episode>? = null,
  val isSignedIn: Boolean = false,
  val lastWatchedAt: ZonedDateTime? = null,
  val rating: RatingState? = null,
  val translation: Translation? = null,
  val dateFormat: DateTimeFormatter? = null,
  val spoilers: SpoilersSettings? = null,
)
