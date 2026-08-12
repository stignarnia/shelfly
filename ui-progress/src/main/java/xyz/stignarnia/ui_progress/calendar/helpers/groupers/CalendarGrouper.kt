package xyz.stignarnia.ui_progress.calendar.helpers.groupers

import xyz.stignarnia.ui_progress.calendar.recycler.CalendarListItem

interface CalendarGrouper {
  fun groupByTime(items: List<CalendarListItem.Episode>): List<CalendarListItem>
}
