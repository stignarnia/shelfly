package xyz.stignarnia.uiShow.sections.seasons.recycler

import xyz.stignarnia.uiModel.RatingState
import xyz.stignarnia.uiModel.Season
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiShow.episodes.recycler.EpisodeListItem

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
