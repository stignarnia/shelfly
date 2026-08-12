@file:Suppress("ktlint:standard:filename")

package com.michaldrabik.ui_show.sections.seasons

import com.michaldrabik.ui_base.utilities.events.Event
import com.michaldrabik.ui_model.IdTmdb
import com.michaldrabik.ui_model.Season
import com.michaldrabik.ui_show.quicksetup.QuickSetupListItem

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
