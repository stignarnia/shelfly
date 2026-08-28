package xyz.stignarnia.uiEpisodes.details

import xyz.stignarnia.uiModel.Episode
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.RatingState
import xyz.stignarnia.uiModel.SpoilersSettings
import xyz.stignarnia.uiModel.Translation
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
