package xyz.stignarnia.ui_widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_base.common.AppScopeProvider
import xyz.stignarnia.ui_model.Settings
import xyz.stignarnia.ui_widgets.theme.WidgetCollection
import xyz.stignarnia.ui_widgets.theme.WidgetPalette
import xyz.stignarnia.ui_widgets.theme.WidgetPalettes
import xyz.stignarnia.ui_widgets.theme.setBackground
import xyz.stignarnia.ui_widgets.theme.setBackgroundTint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

abstract class BaseWidgetProvider : AppWidgetProvider() {

  companion object {
    const val ACTION_CLICK = "ACTION_CLICK"
    const val EXTRA_MODE_CLICK = "EXTRA_MODE_CLICK"
    const val EXTRA_MORE_CLICK = "EXTRA_MORE_CLICK"
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
   * Runs the update off the broadcast thread, and holds the broadcast open until it is done.
   *
   * The rows travel inside the views now - see [WidgetCollection] - so building them means loading from the database and decoding posters, which is far too much for onUpdate to do inline.
   * goAsync is what keeps the receiver alive across that; the application's scope is what it runs in, since the receiver itself is gone the moment onReceive returns.
   */
  protected fun Context.updateAsync(block: suspend () -> Unit) {
    val pendingResult = goAsync()
    (applicationContext as AppScopeProvider).appScope.launch {
      try {
        withContext(Dispatchers.IO) { block() }
      } catch (error: Throwable) {
        Timber.e(error, "Widget update failed.")
      } finally {
        pendingResult.finish()
      }
    }
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
