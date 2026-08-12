package xyz.stignarnia.ui_show.sections.seasons.recycler

import xyz.stignarnia.ui_model.RatingState
import xyz.stignarnia.ui_model.Season
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_show.episodes.recycler.EpisodeListItem

data class SeasonListItem(
  val show: Show,
  val season: Season,
  val episodes: List<EpisodeListItem>,
  val isWatched: Boolean,
  val isRatingHidden: Boolean,
  val isRatingTapToReveal: Boolean,
  val userRating: RatingState,
  val updatedAt: Long,
) {

  val id = season.ids.tmdb.id
}
