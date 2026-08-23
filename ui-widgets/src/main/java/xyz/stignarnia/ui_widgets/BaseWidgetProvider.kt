package xyz.stignarnia.ui_widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_model.Settings
import xyz.stignarnia.ui_widgets.theme.WidgetPalette
import xyz.stignarnia.ui_widgets.theme.WidgetPalettes
import xyz.stignarnia.ui_widgets.theme.setBackground
import xyz.stignarnia.ui_widgets.theme.setBackgroundTint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

abstract class BaseWidgetProvider : AppWidgetProvider() {

  companion object {
    const val ACTION_CLICK = "ACTION_CLICK"
    const val EXTRA_MODE_CLICK = "EXTRA_MODE_CLICK"
    const val EXTRA_SHOW_ID = "EXTRA_SHOW_ID"
    const val EXTRA_MOVIE_ID = "EXTRA_MOVIE_ID"
  }

  @Inject lateinit var settingsRepository: SettingsRepository
  protected lateinit var settings: Settings

  abstract fun getLayoutResId(): Int

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray?,
  ) {
    requireSettings()
    super.onUpdate(context, appWidgetManager, appWidgetIds)
  }

  /**
   * The colours this widget is drawn in, or null where the version cannot be themed - see [WidgetPalettes].
   * Resolved per widget id rather than once per provider, because two copies of the same widget are configured apart from each other.
   */
  protected fun palette(
    context: Context,
    widgetId: Int,
  ): WidgetPalette? = WidgetPalettes.resolve(context, widgetId, settingsRepository)

  /**
   * The frame every list widget shares: the rounded ground and the label bar over it.
   *
   * The background resource is set whatever the palette, because that is what the widget did before it could be themed and it is still what an unthemed one needs.
   * Everything after the guard is the theming.
   */
  protected fun RemoteViews.applyWidgetChrome(
    palette: WidgetPalette?,
    rootId: Int,
    labelId: Int,
    labelTextId: Int,
  ) {
    setInt(rootId, "setBackgroundResource", R.drawable.bg_widget)
    if (palette == null) return

    setBackgroundTint(rootId, palette.background)
    setBackground(labelId, R.drawable.bg_widget_toolbar_tintable, palette.statusBackground)
    setTextColor(labelTextId, palette.statusText)
  }

  /**
   * Sends the widget's views in two parts, because a widget with a scrolling list cannot be updated in one.
   *
   * The framework decides per RemoteViews whether to hand it to the host or merely to tell the host that something changed: RemoteViews carrying a collection bound the old way - setRemoteAdapter with an Intent, which is what a RemoteViewsService is - are never delivered, only announced, and the host picks them up whenever it next inflates the widget.
   * That is why a themed widget appeared to ignore every change and then come back correct after an app update, and why the labels switch and the calendar's empty state looked equally stuck.
   *
   * Nothing requires every update to carry the adapter, though. [buildViews] is asked for the views twice:
   *
   * 1. Without the adapter. That is the whole frame - colours, the label bar, the paddings, the empty state - and with no adapter on it, it is handed to the host. It names the layout the host is already showing, so the host re-applies onto that view tree rather than building a new one, and the list keeps the adapter it already has.
   * 2. With it, restoring the stored copy the host reads when it does inflate again - after a reboot, or when the launcher restarts. Its own announcement is ignored, which no longer matters.
   *
   * The order is the point. Reversed, the stored copy is left without its adapter and the list comes back empty the next time the widget is inflated.
   *
   * Only where there is a palette, which is to say from API 31 - see [WidgetPalettes]. Below that the widget is not themed and this is not its problem to solve: it keeps the single update it has always sent.
   */
  protected fun AppWidgetManager.updateWidget(
    widgetId: Int,
    palette: WidgetPalette?,
    buildViews: (withAdapter: Boolean) -> RemoteViews,
  ) {
    if (palette != null) {
      updateAppWidget(widgetId, buildViews(false))
    }
    updateAppWidget(widgetId, buildViews(true))
  }

  /**
   * Forgets what was stored for widgets the launcher has just removed.
   * The ids come back to the provider that owned them, and nothing else will tell us they are gone.
   */
  override fun onDeleted(
    context: Context,
    appWidgetIds: IntArray?,
  ) {
    appWidgetIds?.forEach { settingsRepository.widgets.clearWidget(it) }
    super.onDeleted(context, appWidgetIds)
  }

  private fun requireSettings() {
    if (!this::settings.isInitialized) {
      settings = runBlocking { settingsRepository.load() }
    }
  }
}
