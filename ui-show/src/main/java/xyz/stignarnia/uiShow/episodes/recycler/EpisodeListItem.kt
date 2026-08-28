package xyz.stignarnia.uiShow.episodes.recycler

import xyz.stignarnia.uiModel.Episode
import xyz.stignarnia.uiModel.Season
import xyz.stignarnia.uiModel.SpoilersSettings
import xyz.stignarnia.uiModel.Translation
import xyz.stignarnia.uiModel.UserRating
import java.time.format.DateTimeFormatter

data class EpisodeListItem(
  val episode: Episode,
  val season: Season,
  val isWatched: Boolean,
  val translation: Translation? = null,
  val myRating: UserRating? = null,
  val dateFormat: DateTimeFormatter? = null,
  val isLocked: Boolean = true,
  val isAnime: Boolean = false,
  val spoilers: SpoilersSettings,
) {
  val id = episode.ids.tmdb.id
}
