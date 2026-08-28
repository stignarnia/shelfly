package xyz.stignarnia.uiProgress.calendar.helpers.groupers

import xyz.stignarnia.uiProgress.calendar.recycler.CalendarListItem

interface CalendarGrouper {
  fun groupByTime(items: List<CalendarListItem.Episode>): List<CalendarListItem>
}
