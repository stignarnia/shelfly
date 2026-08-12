package xyz.stignarnia.ui_show.episodes.recycler

import xyz.stignarnia.ui_model.Episode
import xyz.stignarnia.ui_model.Season
import xyz.stignarnia.ui_model.SpoilersSettings
import xyz.stignarnia.ui_model.UserRating
import xyz.stignarnia.ui_model.Translation
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
