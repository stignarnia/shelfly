
package xyz.stignarnia.uiShow.sections.seasons

import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Season
import xyz.stignarnia.uiShow.quicksetup.QuickSetupListItem

sealed class ShowDetailsSeasonsEvent<T>(
  action: T,
) : Event<T>(action) {
  data class OpenSeasonEpisodes(
    val showId: IdTmdb,
    val seasonId: IdTmdb,
  ) : ShowDetailsSeasonsEvent<IdTmdb>(showId)

  data class OpenSeasonDateSelection(
    val season: Season,
  ) : ShowDetailsSeasonsEvent<Season>(season)

  data class OpenQuickProgressDateSelection(
    val item: QuickSetupListItem,
  ) : ShowDetailsSeasonsEvent<QuickSetupListItem>(item)

  object RequestWidgetsUpdate : ShowDetailsSeasonsEvent<Unit>(Unit)
}
