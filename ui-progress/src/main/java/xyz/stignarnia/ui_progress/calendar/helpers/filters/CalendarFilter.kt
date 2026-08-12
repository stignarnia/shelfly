package xyz.stignarnia.ui_progress.calendar.helpers.filters

import xyz.stignarnia.data_local.database.model.Episode
import java.time.ZonedDateTime

interface CalendarFilter {
  fun filter(
    now: ZonedDateTime,
    episode: Episode,
    onlyPremieres: Boolean,
  ): Boolean
}
