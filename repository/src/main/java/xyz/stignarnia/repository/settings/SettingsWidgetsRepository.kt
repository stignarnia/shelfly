package xyz.stignarnia.repository.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import xyz.stignarnia.common.Mode
import xyz.stignarnia.ui_model.CalendarMode
import xyz.stignarnia.ui_model.WidgetAmoled
import xyz.stignarnia.ui_model.WidgetTheme
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * What each widget on the home screen was told to do, keyed by its widget id.
 *
 * Every key here is a prefix with the widget id appended, because the same provider can be placed several times and the two copies are configured apart from each other.
 * The launcher hands the id back on delete, which is when [clearWidget] takes the row out again - without it a home screen that has had widgets added and removed for a year carries a preference for every one of them forever.
 */
@Singleton
class SettingsWidgetsRepository @Inject constructor(
  @Named("miscPreferences") private var preferences: SharedPreferences,
) {

  companion object Key {
    private const val WIDGET_CALENDAR_MODE = "WIDGET_CALENDAR_MODE"
    private const val WIDGET_CALENDAR_MOVIES_MODE = "WIDGET_CALENDAR_MOVIES_MODE"
    private const val WIDGET_THEME = "WIDGET_THEME"
    private const val WIDGET_AMOLED = "WIDGET_AMOLED"
    private const val WIDGET_TRANSPARENCY = "WIDGET_TRANSPARENCY"
    const val DEFAULT_WIDGET_TRANSPARENCY = 0
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

  fun getWidgetTheme(widgetId: Int): WidgetTheme {
    val stored = preferences.getString("$WIDGET_THEME$widgetId", null)
    return WidgetTheme.fromName(stored)
  }

  fun setWidgetTheme(
    widgetId: Int,
    theme: WidgetTheme,
  ) {
    preferences.edit(true) { putString("$WIDGET_THEME$widgetId", theme.name) }
  }

  fun getWidgetAmoled(widgetId: Int): WidgetAmoled {
    val stored = preferences.getString("$WIDGET_AMOLED$widgetId", null)
    return WidgetAmoled.fromName(stored)
  }

  fun setWidgetAmoled(
    widgetId: Int,
    amoled: WidgetAmoled,
  ) {
    preferences.edit(true) { putString("$WIDGET_AMOLED$widgetId", amoled.name) }
  }

  fun getWidgetTransparency(widgetId: Int): Int =
    preferences.getInt("$WIDGET_TRANSPARENCY$widgetId", DEFAULT_WIDGET_TRANSPARENCY)

  fun setWidgetTransparency(
    widgetId: Int,
    transparency: Int,
  ) {
    preferences.edit(true) { putInt("$WIDGET_TRANSPARENCY$widgetId", transparency.coerceIn(0, 100)) }
  }

  /**
   * Forgets everything stored for a widget the launcher has just removed.
   * Every key this class writes has to be listed here, or it outlives the widget it belonged to.
   */
  fun clearWidget(widgetId: Int) {
    preferences.edit(true) {
      remove("$WIDGET_CALENDAR_MODE$widgetId")
      remove("$WIDGET_CALENDAR_MOVIES_MODE$widgetId")
      remove("$WIDGET_THEME$widgetId")
      remove("$WIDGET_AMOLED$widgetId")
      remove("$WIDGET_TRANSPARENCY$widgetId")
    }
  }
}
