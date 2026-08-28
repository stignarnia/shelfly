package xyz.stignarnia.uiWidgets.calendarMovies

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import xyz.stignarnia.common.Mode
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiBase.utilities.extensions.capitalizeWords
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.replace
import xyz.stignarnia.uiModel.CalendarMode
import xyz.stignarnia.uiModel.ImageStatus
import xyz.stignarnia.uiProgressMovies.calendar.cases.items.CalendarMoviesFutureCase
import xyz.stignarnia.uiProgressMovies.calendar.cases.items.CalendarMoviesRecentsCase
import xyz.stignarnia.uiProgressMovies.calendar.recycler.CalendarMovieListItem
import xyz.stignarnia.uiWidgets.BaseWidgetProvider
import xyz.stignarnia.uiWidgets.BaseWidgetProvider.Companion.EXTRA_MOVIE_ID
import xyz.stignarnia.uiWidgets.R
import xyz.stignarnia.uiWidgets.theme.WidgetPalette
import xyz.stignarnia.uiWidgets.theme.WidgetPalettes
import xyz.stignarnia.uiWidgets.theme.WidgetPosters
import xyz.stignarnia.uiWidgets.theme.setIconTint
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
  private var posters: Map<Long, android.graphics.Bitmap> = emptyMap()

  /**
   * What the row at [position] costs the widget's bitmap budget, known from the poster's size without having fetched it.
   * A row with no poster - a header, or an entry whose image is missing - costs nothing, and charging it one would shorten the list for nothing.
   */
  fun posterCost(position: Int): Long {
    val item = adapterItems.getOrNull(position) ?: return 0
    val image = imageOf(item) ?: return 0
    if (image.status != ImageStatus.AVAILABLE) return 0
    return WidgetPosters.costOf(imageWidth, imageHeight)
  }

  /** Fetches the posters for the rows that were counted, together rather than one at a time - see [WidgetPosters]. */
  suspend fun loadPosters(upTo: Int) {
    val urls =
      (0 until minOf(upTo, adapterItems.size))
        .mapNotNull { position ->
          val item = adapterItems[position]
          val image = imageOf(item) ?: return@mapNotNull null
          if (image.status != ImageStatus.AVAILABLE) return@mapNotNull null
          idOf(item)?.let { it to image.fullFileUrl }
        }.toMap()
    posters = WidgetPosters.fetch(context, imageWidth, imageHeight, imageCorner, urls)
  }

  private val adapterItems = mutableListOf<CalendarMovieListItem>()

  suspend fun load() {
    palette = WidgetPalettes.resolve(context, widgetId, settingsRepository)
    mode = settingsRepository.widgets.getWidgetCalendarMode(Mode.MOVIES, widgetId)
    val items =
      when (mode) {
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
      val fillIntent =
        Intent().apply {
          putExtra(BaseWidgetProvider.EXTRA_MODE_CLICK, true)
          putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
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

    val date =
      if (item.movie.released != null) {
        item.dateFormat?.format(item.movie.released)?.capitalizeWords()
      } else {
        context.getString(R.string.textTba)
      }

    val remoteView =
      RemoteViews(context.packageName, getItemLayout()).apply {
        setTextViewText(R.id.calendarMoviesWidgetItemTitle, title)
        setTextViewText(R.id.calendarMoviesWidgetItemOverview, overview)
        setViewVisibility(R.id.calendarMoviesWidgetItemOverview, if (overview.isBlank()) GONE else VISIBLE)
        setTextViewText(R.id.calendarMoviesWidgetItemDate, date)

        val fillIntent =
          Intent().apply {
            putExtra(EXTRA_MOVIE_ID, item.movie.tmdbId)
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

    // The poster, if it has arrived. The counting pass runs before any of them are fetched, so a row without one keeps its placeholder and is filled in by the pass that follows.
    val bitmap = posters[item.movie.tmdbId]
    if (bitmap == null) {
      remoteView.setViewVisibility(R.id.calendarMoviesWidgetItemImage, GONE)
      remoteView.setViewVisibility(R.id.calendarMoviesWidgetItemPlaceholder, VISIBLE)
    } else {
      remoteView.setImageViewBitmap(R.id.calendarMoviesWidgetItemImage, bitmap)
      remoteView.setViewVisibility(R.id.calendarMoviesWidgetItemImage, VISIBLE)
      remoteView.setViewVisibility(R.id.calendarMoviesWidgetItemPlaceholder, GONE)
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
        Intent().putExtra(BaseWidgetProvider.EXTRA_MORE_CLICK, true),
      )
    }

  private fun imageOf(item: CalendarMovieListItem) = (item as? CalendarMovieListItem.MovieItem)?.image

  private fun idOf(item: CalendarMovieListItem) = (item as? CalendarMovieListItem.MovieItem)?.movie?.tmdbId

  fun itemId(position: Int) = adapterItems[position].movie.tmdbId

  val count get() = adapterItems.size

  val viewTypeCount = 5

  private companion object {
    const val POSTER_TIMEOUT_SECONDS = 2L
  }
}
