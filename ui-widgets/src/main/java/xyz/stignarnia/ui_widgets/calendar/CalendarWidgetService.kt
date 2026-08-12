package xyz.stignarnia.ui_widgets.calendar

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.widget.RemoteViewsService
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_progress.calendar.cases.items.CalendarFutureCase
import xyz.stignarnia.ui_progress.calendar.cases.items.CalendarRecentsCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CalendarWidgetService : RemoteViewsService() {

  @Inject lateinit var calendarFutureCase: CalendarFutureCase
  @Inject lateinit var calendarRecentsCase: CalendarRecentsCase
  @Inject lateinit var settingsRepository: SettingsRepository

  override fun onGetViewFactory(intent: Intent?): CalendarWidgetViewsFactory {
    val widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID) ?: 0
    return CalendarWidgetViewsFactory(
      widgetId,
      applicationContext,
      calendarFutureCase,
      calendarRecentsCase,
      settingsRepository,
    )
  }
}
