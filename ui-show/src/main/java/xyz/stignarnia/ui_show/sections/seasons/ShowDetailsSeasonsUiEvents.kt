@file:Suppress("ktlint:standard:filename")

package xyz.stignarnia.ui_show.sections.seasons

import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.Season
import xyz.stignarnia.ui_show.quicksetup.QuickSetupListItem

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
