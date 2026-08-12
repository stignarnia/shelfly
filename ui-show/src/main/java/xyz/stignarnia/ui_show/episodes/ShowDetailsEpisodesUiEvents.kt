@file:Suppress("ktlint:standard:filename")

package xyz.stignarnia.ui_show.episodes

import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_model.Episode
import xyz.stignarnia.ui_model.EpisodeBundle
import xyz.stignarnia.ui_model.Season
import xyz.stignarnia.ui_show.sections.seasons.recycler.SeasonListItem

sealed class ShowDetailsEpisodesEvent<T>(
  action: T,
) : Event<T>(action) {

  data class OpenEpisodeDetails(
    val bundle: EpisodeBundle,
    val isWatched: Boolean,
  ) : ShowDetailsEpisodesEvent<EpisodeBundle>(bundle)

  data class OpenRateSeason(
    val season: Season,
  ) : ShowDetailsEpisodesEvent<Season>(season)

  data class OpenEpisodeDateSelection(
    val episode: Episode,
  ) : ShowDetailsEpisodesEvent<Episode>(episode)

  data class OpenSeasonDateSelection(
    val season: SeasonListItem,
  ) : ShowDetailsEpisodesEvent<SeasonListItem>(season)

  object RequestWidgetsUpdate : ShowDetailsEpisodesEvent<Unit>(Unit)

  object Finish : ShowDetailsEpisodesEvent<Unit>(Unit)
}
