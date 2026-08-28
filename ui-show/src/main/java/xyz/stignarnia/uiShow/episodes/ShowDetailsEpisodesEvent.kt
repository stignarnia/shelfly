
package xyz.stignarnia.uiShow.episodes

import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiModel.Episode
import xyz.stignarnia.uiModel.EpisodeBundle
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Season
import xyz.stignarnia.uiShow.sections.seasons.recycler.SeasonListItem

sealed class ShowDetailsEpisodesEvent<T>(
  action: T,
) : Event<T>(action) {
  data class OpenEpisodeDetails(
    val bundle: EpisodeBundle,
    val isWatched: Boolean,
  ) : ShowDetailsEpisodesEvent<EpisodeBundle>(bundle)

  data class OpenRateSeason(
    val showId: IdTmdb,
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
