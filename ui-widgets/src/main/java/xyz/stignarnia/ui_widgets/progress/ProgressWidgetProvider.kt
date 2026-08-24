package xyz.stignarnia.ui_widgets.progress

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS
import android.appwidget.AppWidgetManager.getInstance
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.URI_INTENT_SCHEME
import android.net.Uri
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RemoteViews
import xyz.stignarnia.common.Config.HOST_ACTIVITY_NAME
import xyz.stignarnia.repository.EpisodesManager
import xyz.stignarnia.ui_base.common.WidgetsProvider
import xyz.stignarnia.ui_base.utilities.AndroidVersion
import xyz.stignarnia.ui_base.utilities.extensions.dimenToPx
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_widgets.BaseWidgetProvider
import xyz.stignarnia.ui_progress.progress.cases.ProgressItemsCase
import xyz.stignarnia.ui_widgets.R
import xyz.stignarnia.ui_widgets.theme.WidgetCollection
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ProgressWidgetProvider : BaseWidgetProvider() {

  @Inject lateinit var progressItemsCase: ProgressItemsCase
  @Inject lateinit var episodesManager: EpisodesManager

  companion object {
    const val EXTRA_SEASON_ID = "EXTRA_SEASON_ID"
    const val EXTRA_EPISODE_ID = "EXTRA_EPISODE_ID"

    fun requestUpdate(context: Context) {
      val applicationContext = context.applicationContext
      val intent = Intent(applicationContext, ProgressWidgetProvider::class.java).apply {
        val ids: IntArray = getInstance(applicationContext)
          .getAppWidgetIds(ComponentName(applicationContext, ProgressWidgetProvider::class.java))
        action = ACTION_APPWIDGET_UPDATE
        putExtra(EXTRA_APPWIDGET_IDS, ids)
      }
      applicationContext.sendBroadcast(intent)
      Timber.d("Widget update requested.")
    }
  }

  override fun getLayoutResId(): Int = R.layout.widget_progress_night

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

    val palette = palette(context, widgetId)

    val mainIntent = PendingIntent.getActivity(
      context,
      0,
      Intent().apply { setClassName(context, HOST_ACTIVITY_NAME) },
      FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT,
    )

    val listClickIntent = Intent(context, ProgressWidgetProvider::class.java).apply {
      action = ACTION_CLICK
    }
    val showDetailsPendingIntent = PendingIntent.getBroadcast(
      context,
      0,
      listClickIntent,
      FLAG_MUTABLE or FLAG_UPDATE_CURRENT,
    )

    context.updateAsync {
      val rows = ProgressWidgetRows(widgetId, context, progressItemsCase, settingsRepository)
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
          setRemoteAdapter(R.id.progressWidgetList, items)
          setEmptyView(R.id.progressWidgetList, R.id.progressWidgetEmptyView)

          val spaceTiny = context.dimenToPx(R.dimen.spaceTiny)
          val paddingTop = if (settings.widgetsShowLabel) context.dimenToPx(R.dimen.widgetPaddingTop) else spaceTiny
          val labelVisibility = if (settings.widgetsShowLabel) VISIBLE else GONE
          setViewPadding(R.id.progressWidgetList, 0, paddingTop, 0, spaceTiny)
          setViewVisibility(R.id.progressWidgetLabel, labelVisibility)

          applyWidgetChrome(
            palette,
            R.id.progressWidgetNightRoot,
            R.id.progressWidgetLabel,
            R.id.progressWidgetLabelText,
          )
          palette?.let {
            setTextColor(R.id.progressWidgetEmptyViewTitle, it.textPrimary)
            setTextColor(R.id.progressWidgetEmptyViewSubtitle, it.textSecondary)
          }

          setOnClickPendingIntent(R.id.progressWidgetLabel, mainIntent)
          setPendingIntentTemplate(R.id.progressWidgetList, showDetailsPendingIntent)
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

  override fun onReceive(
    context: Context,
    intent: Intent,
  ) {
    super.onReceive(context, intent)
    if (intent.action.equals(ACTION_CLICK)) {
      when {
        intent.extras?.containsKey(EXTRA_MORE_CLICK) == true -> {
          // The row standing in for what did not fit: open the app where the header does.
          context.startActivity(
            Intent().apply {
              setClassName(context, HOST_ACTIVITY_NAME)
              flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
          )
        }
        intent.extras?.containsKey(EXTRA_EPISODE_ID) == true -> {
          val episodeId = intent.getLongExtra(EXTRA_EPISODE_ID, -1L)
          val seasonId = intent.getLongExtra(EXTRA_SEASON_ID, -1L)
          val showId = intent.getLongExtra(EXTRA_SHOW_ID, -1L)
          if (episodeId != -1L && seasonId != -1L && showId != -1L) {
            val pendingResult = goAsync()
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
              try {
                episodesManager.setEpisodeWatched(episodeId, seasonId, IdTmdb(showId), null)
                (appContext as? WidgetsProvider)?.requestShowsWidgetsUpdate()
              } finally {
                pendingResult.finish()
              }
            }
          }
        }
        intent.extras?.containsKey(EXTRA_SHOW_ID) == true -> {
          val showId = intent.getLongExtra(EXTRA_SHOW_ID, -1L)
          context.startActivity(
            Intent().apply {
              setClassName(context, HOST_ACTIVITY_NAME)
              putExtra(EXTRA_SHOW_ID, showId.toString())
              flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
          )
        }
      }
    }
  }
}
