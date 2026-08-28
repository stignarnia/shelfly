package xyz.stignarnia.uiProgress.calendar.helpers.filters

import xyz.stignarnia.dataLocal.database.model.Episode
import java.time.ZonedDateTime

interface CalendarFilter {
  fun filter(
    now: ZonedDateTime,
    episode: Episode,
    onlyPremieres: Boolean,
  ): Boolean
}
