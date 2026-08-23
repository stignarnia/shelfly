package xyz.stignarnia.ui_widgets.calendar_movies

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RemoteViews
import androidx.core.os.bundleOf
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import xyz.stignarnia.common.Mode
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_base.utilities.extensions.capitalizeWords
import xyz.stignarnia.ui_base.utilities.extensions.dimenToPx
import xyz.stignarnia.ui_base.utilities.extensions.replace
import xyz.stignarnia.ui_model.CalendarMode
import xyz.stignarnia.ui_model.ImageStatus
import xyz.stignarnia.ui_progress_movies.calendar.cases.items.CalendarMoviesFutureCase
import xyz.stignarnia.ui_progress_movies.calendar.cases.items.CalendarMoviesRecentsCase
import xyz.stignarnia.ui_progress_movies.calendar.recycler.CalendarMovieListItem
import xyz.stignarnia.ui_widgets.BaseWidgetProvider
import xyz.stignarnia.ui_widgets.BaseWidgetProvider.Companion.EXTRA_MOVIE_ID
import xyz.stignarnia.ui_widgets.R
import xyz.stignarnia.ui_widgets.theme.WidgetPalette
import xyz.stignarnia.ui_widgets.theme.WidgetPalettes
import xyz.stignarnia.ui_widgets.theme.setIconTint
import java.util.concurrent.TimeUnit

/**
 * The rows of the movies calendar widget, built to be carried inside the widget's own views.
 *
 * Was a RemoteViewsFactory behind a RemoteViewsService, which the launcher called back into per row.
 * The framework never delivers views that name such a service - see WidgetCollection - so the rows are built here instead and travel with the frame.
 */
class CalendarMoviesWidgetRows(
  private val widgetId: Int,
  private val context: Context,
  private val futureItemsCase: CalendarMoviesFutureCase,
  private val recentItemsCase: CalendarMoviesRecentsCase,
  private val settingsRepository: SettingsRepository,
) {

  private val imageCorner by lazy { context.dimenToPx(R.dimen.mediaTileCorner) }
  private val imageWidth by lazy { context.dimenToPx(R.dimen.widgetImageWidth) }
  private val imageHeight by lazy { context.dimenToPx(R.dimen.widgetImageHeight) }
  private var mode = CalendarMode.PRESENT_FUTURE
  private var palette: WidgetPalette? = null

  private val adapterItems = mutableListOf<CalendarMovieListItem>()

  suspend fun load() {
    palette = WidgetPalettes.resolve(context, widgetId, settingsRepository)
    mode = settingsRepository.widgets.getWidgetCalendarMode(Mode.MOVIES, widgetId)
    val items = when (mode) {
      CalendarMode.PRESENT_FUTURE -> futureItemsCase.loadItems(withFilters = false)
      CalendarMode.RECENTS -> recentItemsCase.loadItems(withFilters = false)
    }
    adapterItems.replace(items)
  }

  fun viewAt(position: Int) =
    when (val item = adapterItems[position]) {
      is CalendarMovieListItem.MovieItem -> createItemRemoteView(item)
      is CalendarMovieListItem.Header -> createHeaderRemoteView(item, showIcon = position == 0)
      is CalendarMovieListItem.Filters -> throw IllegalStateException("Filters should not be in the widget")
    }

  private fun createHeaderRemoteView(
    item: CalendarMovieListItem.Header,
    showIcon: Boolean,
  ) = RemoteViews(context.packageName, getHeaderLayout()).apply {
    setTextViewText(R.id.progressWidgetHeaderTitle, context.getString(item.textResId))
    setViewVisibility(R.id.progressWidgetHeaderTitleIcon, if (mode == CalendarMode.RECENTS) VISIBLE else GONE)

    if (showIcon) {
      when (mode) {
        CalendarMode.PRESENT_FUTURE -> setImageViewResource(R.id.progressWidgetHeaderIcon, R.drawable.ic_history)
        CalendarMode.RECENTS -> setImageViewResource(R.id.progressWidgetHeaderIcon, R.drawable.ic_calendar)
      }
      setViewVisibility(R.id.progressWidgetHeaderIcon, VISIBLE)
      val fillIntent = Intent().apply {
        putExtras(bundleOf(BaseWidgetProvider.EXTRA_MODE_CLICK to true))
        putExtras(bundleOf(AppWidgetManager.EXTRA_APPWIDGET_ID to widgetId))
      }
      setOnClickFillInIntent(R.id.progressWidgetHeaderIcon, fillIntent)
    } else {
      setViewVisibility(R.id.progressWidgetHeaderIcon, GONE)
    }

    palette?.let {
      setTextColor(R.id.progressWidgetHeaderTitle, it.textPrimary)
      setIconTint(R.id.progressWidgetHeaderTitleIcon, it.textPrimary)
      setIconTint(R.id.progressWidgetHeaderIcon, it.textPrimary)
    }
  }

  private fun createItemRemoteView(item: CalendarMovieListItem.MovieItem): RemoteViews {
    val translatedTitle = item.translation?.title
    val title =
      if (translatedTitle?.isBlank() == false) {
        translatedTitle
      } else {
        item.movie.title
      }

    val translatedDescription = item.translation?.overview
    val overview =
      if (translatedDescription?.isBlank() == false) {
        translatedDescription
      } else {
        item.movie.overview
      }

    val date = if (item.movie.released != null) {
      item.dateFormat?.format(item.movie.released)?.capitalizeWords()
    } else {
      context.getString(R.string.textTba)
    }

    val remoteView = RemoteViews(context.packageName, getItemLayout()).apply {
      setTextViewText(R.id.calendarMoviesWidgetItemTitle, title)
      setTextViewText(R.id.calendarMoviesWidgetItemOverview, overview)
      setViewVisibility(R.id.calendarMoviesWidgetItemOverview, if (overview.isBlank()) GONE else VISIBLE)
      setTextViewText(R.id.calendarMoviesWidgetItemDate, date)

      val fillIntent = Intent().apply {
        putExtras(bundleOf(EXTRA_MOVIE_ID to item.movie.tmdbId))
      }
      setOnClickFillInIntent(R.id.calendarMoviesWidgetItem, fillIntent)

      palette?.let {
        setInt(R.id.calendarMoviesWidgetItemFrame, "setBackgroundResource", it.mediaFrame)
        setIconTint(R.id.calendarMoviesWidgetItemPlaceholder, it.placeholderInk)
        setTextColor(R.id.calendarMoviesWidgetItemTitle, it.textPrimary)
        setTextColor(R.id.calendarMoviesWidgetItemOverview, it.textPrimary)
        setTextColor(R.id.calendarMoviesWidgetItemDate, it.textSecondary)
      }
    }

    if (item.image.status != ImageStatus.AVAILABLE) {
      remoteView.setViewVisibility(R.id.calendarMoviesWidgetItemImage, GONE)
      remoteView.setViewVisibility(R.id.calendarMoviesWidgetItemPlaceholder, VISIBLE)
      return remoteView
    }

    try {
      remoteView.setViewVisibility(R.id.calendarMoviesWidgetItemImage, GONE)
      remoteView.setViewVisibility(R.id.calendarMoviesWidgetItemPlaceholder, GONE)

      val bitmap = Glide
        .with(context)
        .asBitmap()
        .load(item.image.fullFileUrl)
        .transform(CenterCrop(), RoundedCorners(imageCorner))
        .submit(imageWidth, imageHeight)
        // Time boxed: every row's poster is fetched before the widget can be sent, so one slow image must not hold the whole list up.
        // A miss falls through to the placeholder and is picked up on the next update, by which point Glide has it cached.
        .get(POSTER_TIMEOUT_SECONDS, TimeUnit.SECONDS)

      remoteView.setImageViewBitmap(R.id.calendarMoviesWidgetItemImage, bitmap)
      remoteView.setViewVisibility(R.id.calendarMoviesWidgetItemImage, VISIBLE)
    } catch (t: Throwable) {
      remoteView.setViewVisibility(R.id.calendarMoviesWidgetItemImage, GONE)
      remoteView.setViewVisibility(R.id.calendarMoviesWidgetItemPlaceholder, VISIBLE)
    }

    return remoteView
  }

  private fun getItemLayout(): Int = R.layout.widget_movies_calendar_item_night

  private fun getHeaderLayout(): Int = R.layout.widget_header_night

  /** The row that stands in for everything that did not fit, opening the app where the widget's header does. */
  fun moreView(): RemoteViews =
    RemoteViews(context.packageName, R.layout.widget_more_item).apply {
      palette?.let { setTextColor(R.id.widgetMoreItemText, it.textSecondary) }
      setOnClickFillInIntent(
        R.id.widgetMoreItem,
        Intent().putExtras(bundleOf(BaseWidgetProvider.EXTRA_MORE_CLICK to true)),
      )
    }

  fun itemId(position: Int) = adapterItems[position].movie.tmdbId

  val count get() = adapterItems.size

  val viewTypeCount = 5

  private companion object {
    const val POSTER_TIMEOUT_SECONDS = 2L
  }
}
