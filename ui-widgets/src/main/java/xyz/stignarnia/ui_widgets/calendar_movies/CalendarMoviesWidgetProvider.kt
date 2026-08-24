package xyz.stignarnia.ui_widgets.calendar_movies

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
import xyz.stignarnia.ui_base.utilities.AndroidVersion
import xyz.stignarnia.ui_base.utilities.extensions.dimenToPx
import xyz.stignarnia.ui_model.CalendarMode
import xyz.stignarnia.ui_progress_movies.calendar.cases.items.CalendarMoviesFutureCase
import xyz.stignarnia.ui_progress_movies.calendar.cases.items.CalendarMoviesRecentsCase
import xyz.stignarnia.ui_widgets.BaseWidgetProvider
import xyz.stignarnia.ui_widgets.R
import xyz.stignarnia.ui_widgets.theme.WidgetCollection
import xyz.stignarnia.ui_widgets.theme.setIconTint
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class CalendarMoviesWidgetProvider : BaseWidgetProvider() {

  @Inject lateinit var calendarMoviesFutureCase: CalendarMoviesFutureCase
  @Inject lateinit var calendarMoviesRecentsCase: CalendarMoviesRecentsCase

  companion object {
    fun requestUpdate(context: Context) {
      val applicationContext = context.applicationContext
      val intent = Intent(applicationContext, CalendarMoviesWidgetProvider::class.java).apply {
        val ids: IntArray = AppWidgetManager
          .getInstance(applicationContext)
          .getAppWidgetIds(ComponentName(applicationContext, CalendarMoviesWidgetProvider::class.java))
        action = ACTION_APPWIDGET_UPDATE
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
      }
      applicationContext.sendBroadcast(intent)
      Timber.d("Widget update requested.")
    }
  }

  override fun getLayoutResId(): Int = R.layout.widget_movies_calendar_night

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
    if (!AndroidVersion.isAtLeastAndroid12) return

    val spaceTiny = context.dimenToPx(R.dimen.spaceTiny)

    val palette = palette(context, widgetId)

    val listClickIntent = Intent(context, CalendarMoviesWidgetProvider::class.java).apply {
      action = ACTION_CLICK
    }
    val listIntent = PendingIntent.getBroadcast(context, 0, listClickIntent, FLAG_MUTABLE or FLAG_UPDATE_CURRENT)

    val mainIntent = PendingIntent.getActivity(
      context,
      0,
      Intent().apply { setClassName(context, Config.HOST_ACTIVITY_NAME) },
      FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT,
    )

    val modeClickIntent = PendingIntent.getBroadcast(
      context,
      2,
      Intent(ACTION_CLICK).apply {
        setClass(context, this@CalendarMoviesWidgetProvider.javaClass)
        putExtra(EXTRA_MODE_CLICK, true)
        putExtra(EXTRA_APPWIDGET_ID, widgetId)
      },
      FLAG_MUTABLE or FLAG_UPDATE_CURRENT,
    )

    context.updateAsync {
      val rows = CalendarMoviesWidgetRows(
        widgetId,
        context,
        calendarMoviesFutureCase,
        calendarMoviesRecentsCase,
        settingsRepository,
      )
      rows.load()
      // First pass: no posters yet, so it costs nothing to wait for. This is what decides how many rows fit.
      val (placeholders, taken) = WidgetCollection.fill(
        context,
        rows.count,
        rows.viewTypeCount,
        rows::posterCost,
        rows::moreView,
      ) { position -> rows.itemId(position) to rows.viewAt(position) }
      Timber.d("Widget $widgetId building $taken of ${rows.count} rows.")

      fun remoteViews(items: RemoteViews.RemoteCollectionItems) =
        RemoteViews(context.packageName, getLayoutResId()).apply {
          setRemoteAdapter(R.id.calendarWidgetMoviesList, items)
          setEmptyView(R.id.calendarWidgetMoviesList, R.id.calendarWidgetMoviesEmptyView)

          val paddingTop = if (settings.widgetsShowLabel) context.dimenToPx(R.dimen.widgetPaddingTop) else spaceTiny
          val labelVisibility = if (settings.widgetsShowLabel) VISIBLE else GONE
          setViewPadding(R.id.calendarWidgetMoviesList, 0, paddingTop, 0, spaceTiny)
          setViewPadding(R.id.calendarWidgetMoviesEmptyView, 0, paddingTop, 0, 0)
          setViewVisibility(R.id.calendarWidgetMoviesLabel, labelVisibility)

          applyWidgetChrome(
            palette,
            R.id.calendarWidgetMoviesNightRoot,
            R.id.calendarWidgetMoviesLabel,
            R.id.calendarWidgetMoviesLabelText,
          )
          palette?.let {
            setTextColor(R.id.calendarWidgetMoviesEmptyViewTitle, it.textPrimary)
            setTextColor(R.id.calendarWidgetMoviesEmptyViewSubtitle, it.textSecondary)
            setIconTint(R.id.calendarWidgetMoviesEmptyViewIcon, it.textPrimary)
          }

          when (settingsRepository.widgets.getWidgetCalendarMode(Mode.MOVIES, widgetId)) {
            CalendarMode.PRESENT_FUTURE -> {
              setImageViewResource(R.id.calendarWidgetMoviesEmptyViewIcon, R.drawable.ic_history)
              setTextViewText(
                R.id.calendarWidgetMoviesEmptyViewSubtitle,
                context.getString(R.string.textMoviesCalendarEmpty),
              )
            }
            CalendarMode.RECENTS -> {
              setImageViewResource(R.id.calendarWidgetMoviesEmptyViewIcon, R.drawable.ic_calendar)
              setTextViewText(
                R.id.calendarWidgetMoviesEmptyViewSubtitle,
                context.getString(R.string.textMoviesCalendarRecentsEmpty),
              )
            }
          }

          setOnClickPendingIntent(R.id.calendarWidgetMoviesLabelImage, mainIntent)
          setOnClickPendingIntent(R.id.calendarWidgetMoviesLabelText, mainIntent)
          setOnClickPendingIntent(R.id.calendarWidgetMoviesEmptyViewIcon, modeClickIntent)
          setPendingIntentTemplate(R.id.calendarWidgetMoviesList, listIntent)
        }

      appWidgetManager.updateAppWidget(widgetId, remoteViews(placeholders))

      // Second pass: the posters, fetched together, and the same rows sent again with them in place.
      rows.loadPosters(taken)
      val withPosters = WidgetCollection.build(
        (0 until taken).map { position -> rows.itemId(position) to rows.viewAt(position) },
        rows.count,
        rows.viewTypeCount,
        rows::moreView,
      )
      appWidgetManager.updateAppWidget(widgetId, remoteViews(withPosters))
    }
  }

  private fun toggleCalendarMode(widgetId: Int) {
    when (settingsRepository.widgets.getWidgetCalendarMode(Mode.MOVIES, widgetId)) {
      CalendarMode.PRESENT_FUTURE -> {
        settingsRepository.widgets.setWidgetCalendarMode(Mode.MOVIES, widgetId, CalendarMode.RECENTS)
      }
      CalendarMode.RECENTS -> {
        settingsRepository.widgets.setWidgetCalendarMode(Mode.MOVIES, widgetId, CalendarMode.PRESENT_FUTURE)
      }
    }
  }

  override fun onReceive(
    context: Context,
    intent: Intent,
  ) {
    fun onListItemClick() {
      val movieId = intent.getLongExtra(EXTRA_MOVIE_ID, -1L)
      context.startActivity(
        Intent().apply {
          setClassName(context, Config.HOST_ACTIVITY_NAME)
          putExtra(EXTRA_MOVIE_ID, movieId.toString())
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
        intent.extras?.containsKey(EXTRA_MORE_CLICK) == true -> {
          // The row standing in for what did not fit: open the app where the header does.
          context.startActivity(
            Intent().apply {
              setClassName(context, Config.HOST_ACTIVITY_NAME)
              flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
          )
        }
        intent.extras?.containsKey(EXTRA_MOVIE_ID) == true -> {
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
