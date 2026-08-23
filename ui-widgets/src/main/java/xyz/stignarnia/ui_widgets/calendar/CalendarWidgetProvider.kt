package xyz.stignarnia.ui_widgets.calendar

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RemoteViews
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.Mode
import xyz.stignarnia.ui_base.utilities.extensions.dimenToPx
import xyz.stignarnia.ui_model.CalendarMode
import xyz.stignarnia.ui_widgets.BaseWidgetProvider
import xyz.stignarnia.ui_widgets.R
import xyz.stignarnia.ui_widgets.theme.setIconTint
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class CalendarWidgetProvider : BaseWidgetProvider() {

  companion object {
    fun requestUpdate(context: Context) {
      val applicationContext = context.applicationContext
      val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
      val intent = Intent(applicationContext, CalendarWidgetProvider::class.java).apply {
        val ids = appWidgetManager.getAppWidgetIds(
          ComponentName(applicationContext, CalendarWidgetProvider::class.java),
        )
        action = ACTION_APPWIDGET_UPDATE
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
      }
      applicationContext.sendBroadcast(intent)
      Timber.d("Widget update requested.")
    }
  }

  override fun getLayoutResId(): Int = R.layout.widget_calendar_night

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray?,
  ) {
    super.onUpdate(context, appWidgetManager, appWidgetIds)
    appWidgetIds?.forEach { updateWidget(context, appWidgetManager, it) }
  }

  private fun updateWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    widgetId: Int,
  ) {
    val intent = Intent(context, CalendarWidgetService::class.java).apply {
      putExtra(EXTRA_APPWIDGET_ID, widgetId)
      data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
    }

    val palette = palette(context, widgetId)

    val mainIntent = PendingIntent.getActivity(
      context,
      0,
      Intent().apply { setClassName(context, Config.HOST_ACTIVITY_NAME) },
      FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT,
    )

    val modeClickIntent = PendingIntent.getBroadcast(
      context,
      1,
      Intent(ACTION_CLICK).apply {
        setClass(context, this@CalendarWidgetProvider.javaClass)
        putExtra(EXTRA_MODE_CLICK, true)
        putExtra(EXTRA_APPWIDGET_ID, widgetId)
      },
      FLAG_MUTABLE or FLAG_UPDATE_CURRENT,
    )

    val listClickIntent = Intent(context, CalendarWidgetProvider::class.java).apply {
      action = ACTION_CLICK
      data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))
    }
    val listIntent = PendingIntent.getBroadcast(context, 0, listClickIntent, FLAG_MUTABLE or FLAG_UPDATE_CURRENT)

    // Everything but the adapter, so the same frame can be sent with it and without - see updateWidget.
    fun buildViews(withAdapter: Boolean) =
      RemoteViews(context.packageName, getLayoutResId()).apply {
        if (withAdapter) setRemoteAdapter(R.id.calendarWidgetList, intent)
        setEmptyView(R.id.calendarWidgetList, R.id.calendarWidgetEmptyView)

        val spaceTiny = context.dimenToPx(R.dimen.spaceTiny)
        val paddingTop = if (settings.widgetsShowLabel) context.dimenToPx(R.dimen.widgetPaddingTop) else spaceTiny
        val labelVisibility = if (settings.widgetsShowLabel) VISIBLE else GONE
        setViewPadding(R.id.calendarWidgetList, 0, paddingTop, 0, spaceTiny)
        setViewPadding(R.id.calendarWidgetEmptyView, 0, paddingTop, 0, 0)
        setViewVisibility(R.id.calendarWidgetLabel, labelVisibility)

        applyWidgetChrome(palette, R.id.calendarWidgetNightRoot, R.id.calendarWidgetLabel, R.id.calendarWidgetLabelText)
        palette?.let {
          setTextColor(R.id.calendarWidgetEmptyViewTitle, it.textPrimary)
          setTextColor(R.id.calendarWidgetEmptyViewSubtitle, it.textSecondary)
          setIconTint(R.id.calendarWidgetEmptyViewIcon, it.textPrimary)
        }

        when (settingsRepository.widgets.getWidgetCalendarMode(Mode.SHOWS, widgetId)) {
          CalendarMode.PRESENT_FUTURE -> {
            setImageViewResource(R.id.calendarWidgetEmptyViewIcon, R.drawable.ic_history)
            setTextViewText(R.id.calendarWidgetEmptyViewSubtitle, context.getString(R.string.textCalendarEmpty))
          }
          CalendarMode.RECENTS -> {
            setImageViewResource(R.id.calendarWidgetEmptyViewIcon, R.drawable.ic_calendar)
            setTextViewText(R.id.calendarWidgetEmptyViewSubtitle, context.getString(R.string.textRecentsEmpty))
          }
        }

        setOnClickPendingIntent(R.id.calendarWidgetLabelImage, mainIntent)
        setOnClickPendingIntent(R.id.calendarWidgetLabelText, mainIntent)
        setOnClickPendingIntent(R.id.calendarWidgetEmptyViewIcon, modeClickIntent)
        setPendingIntentTemplate(R.id.calendarWidgetList, listIntent)
      }

    appWidgetManager.updateWidget(widgetId, palette, ::buildViews)
    appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.calendarWidgetList)
  }

  private fun toggleCalendarMode(widgetId: Int) {
    when (settingsRepository.widgets.getWidgetCalendarMode(Mode.SHOWS, widgetId)) {
      CalendarMode.PRESENT_FUTURE -> {
        settingsRepository.widgets.setWidgetCalendarMode(Mode.SHOWS, widgetId, CalendarMode.RECENTS)
      }
      CalendarMode.RECENTS -> {
        settingsRepository.widgets.setWidgetCalendarMode(Mode.SHOWS, widgetId, CalendarMode.PRESENT_FUTURE)
      }
    }
  }

  override fun onReceive(
    context: Context,
    intent: Intent,
  ) {
    fun onListItemClick() {
      val showId = intent.getLongExtra(EXTRA_SHOW_ID, -1L)
      context.startActivity(
        Intent().apply {
          setClassName(context, Config.HOST_ACTIVITY_NAME)
          putExtra(EXTRA_SHOW_ID, showId.toString())
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
      )
    }

    fun onHeaderIconClick(widgetId: Int) {
      toggleCalendarMode(widgetId)
      requestUpdate(context.applicationContext)
    }

    super.onReceive(context, intent)
    if (intent.action == ACTION_CLICK) {
      when {
        intent.extras?.containsKey(EXTRA_SHOW_ID) == true -> {
          onListItemClick()
        }
        intent.extras?.containsKey(EXTRA_MODE_CLICK) == true -> {
          val widgetId = intent.extras?.getInt(EXTRA_APPWIDGET_ID) ?: 0
          onHeaderIconClick(widgetId)
        }
      }
    }
  }
}
