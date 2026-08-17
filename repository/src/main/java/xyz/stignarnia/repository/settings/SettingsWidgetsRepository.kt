package xyz.stignarnia.repository.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import xyz.stignarnia.common.Mode
import xyz.stignarnia.ui_model.CalendarMode
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SettingsWidgetsRepository @Inject constructor(
  @Named("miscPreferences") private var preferences: SharedPreferences,
) {

  companion object Key {
    private const val WIDGET_CALENDAR_MODE = "WIDGET_CALENDAR_MODE"
    private const val WIDGET_CALENDAR_MOVIES_MODE = "WIDGET_CALENDAR_MOVIES_MODE"
  }

  fun getWidgetCalendarMode(
    mode: Mode,
    widgetId: Int,
  ): CalendarMode {
    val default = CalendarMode.PRESENT_FUTURE.name
    val key = when (mode) {
      Mode.SHOWS -> WIDGET_CALENDAR_MODE
      Mode.MOVIES -> WIDGET_CALENDAR_MOVIES_MODE
    }
    val value = preferences.getString("$key$widgetId", default) ?: default
    return CalendarMode.valueOf(value)
  }

  fun setWidgetCalendarMode(
    mode: Mode,
    widgetId: Int,
    calendarMode: CalendarMode,
  ) {
    val key = when (mode) {
      Mode.SHOWS -> WIDGET_CALENDAR_MODE
      Mode.MOVIES -> WIDGET_CALENDAR_MOVIES_MODE
    }
    preferences.edit(true) { putString("$key$widgetId", calendarMode.name) }
  }
}
