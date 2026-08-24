package xyz.stignarnia.ui_widgets.search

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import xyz.stignarnia.common.Config.HOST_ACTIVITY_NAME
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_widgets.R
import xyz.stignarnia.ui_widgets.theme.WidgetPalettes
import xyz.stignarnia.ui_widgets.theme.setBackgroundTint
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * The search bar widget.
 *
 * It stands apart from [xyz.stignarnia.ui_widgets.BaseWidgetProvider]: there is no list, no label bar and nothing to read out of the settings table, so it takes the palette directly rather than inheriting a blocking load it has no use for.
 */
@AndroidEntryPoint
class SearchWidgetProvider : AppWidgetProvider() {

  companion object {
    const val EXTRA_WIDGET_SEARCH_CLICK = "EXTRA_WIDGET_SEARCH_CLICK"

    fun requestUpdate(context: Context) {
      val applicationContext = context.applicationContext
      val intent = Intent(applicationContext, SearchWidgetProvider::class.java).apply {
        val ids: IntArray = AppWidgetManager
          .getInstance(applicationContext)
          .getAppWidgetIds(ComponentName(applicationContext, SearchWidgetProvider::class.java))
        action = ACTION_APPWIDGET_UPDATE
        putExtra(EXTRA_APPWIDGET_IDS, ids)
      }
      applicationContext.sendBroadcast(intent)
      Timber.d("Widget update requested.")
    }
  }

  @Inject lateinit var settingsRepository: SettingsRepository

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray?,
  ) {
    appWidgetIds?.forEach { updateWidget(context, appWidgetManager, it) }
    super.onUpdate(context, appWidgetManager, appWidgetIds)
  }

  override fun onDeleted(
    context: Context,
    appWidgetIds: IntArray?,
  ) {
    appWidgetIds?.forEach { settingsRepository.widgets.clearWidget(it) }
    super.onDeleted(context, appWidgetIds)
  }

  private fun updateWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    widgetId: Int,
  ) {
    val palette = WidgetPalettes.resolve(context, widgetId, settingsRepository)

    val remoteViews = RemoteViews(context.packageName, R.layout.widget_search).apply {
      val intent = Intent().apply {
        setClassName(context, HOST_ACTIVITY_NAME)
        putExtra(EXTRA_WIDGET_SEARCH_CLICK, true)
      }
      val pendingIntent = PendingIntent.getActivity(context, 2, intent, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)
      setOnClickPendingIntent(R.id.searchWidgetRoot, pendingIntent)

      palette.let {
        setBackgroundTint(R.id.searchWidgetRoot, it.searchBackground)
        setTextColor(R.id.searchWidgetText, it.textSecondary)
      }
    }
    appWidgetManager.updateAppWidget(widgetId, remoteViews)
  }
}
