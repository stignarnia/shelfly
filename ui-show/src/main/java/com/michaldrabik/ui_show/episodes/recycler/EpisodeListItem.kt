package com.michaldrabik.ui_show.episodes.recycler

import com.michaldrabik.ui_model.Episode
import com.michaldrabik.ui_model.Season
import com.michaldrabik.ui_model.SpoilersSettings
import com.michaldrabik.ui_model.UserRating
import com.michaldrabik.ui_model.Translation
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
