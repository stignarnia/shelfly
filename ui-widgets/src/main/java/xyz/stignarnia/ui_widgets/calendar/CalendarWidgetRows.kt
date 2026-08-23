package xyz.stignarnia.ui_widgets.calendar

import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID
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
import xyz.stignarnia.common.extensions.toLocalZone
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_base.utilities.extensions.capitalizeWords
import xyz.stignarnia.ui_base.utilities.extensions.dimenToPx
import xyz.stignarnia.ui_base.utilities.extensions.replace
import xyz.stignarnia.ui_model.CalendarMode.PRESENT_FUTURE
import xyz.stignarnia.ui_model.CalendarMode.RECENTS
import xyz.stignarnia.ui_model.ImageStatus
import xyz.stignarnia.ui_progress.calendar.cases.items.CalendarFutureCase
import xyz.stignarnia.ui_progress.calendar.cases.items.CalendarRecentsCase
import xyz.stignarnia.ui_progress.calendar.recycler.CalendarListItem
import xyz.stignarnia.ui_widgets.BaseWidgetProvider.Companion.EXTRA_MODE_CLICK
import xyz.stignarnia.ui_widgets.BaseWidgetProvider.Companion.EXTRA_SHOW_ID
import xyz.stignarnia.ui_widgets.BaseWidgetProvider
import xyz.stignarnia.ui_widgets.R
import xyz.stignarnia.ui_widgets.theme.WidgetPalette
import xyz.stignarnia.ui_widgets.theme.WidgetPalettes
import xyz.stignarnia.ui_widgets.theme.WidgetPosters
import xyz.stignarnia.ui_widgets.theme.setBackgroundTint
import xyz.stignarnia.ui_widgets.theme.setIconTint
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * The rows of the shows calendar widget, built to be carried inside the widget's own views.
 *
 * Was a RemoteViewsFactory behind a RemoteViewsService, which the launcher called back into per row.
 * The framework never delivers views that name such a service - see WidgetCollection - so the rows are built here instead and travel with the frame.
 */
class CalendarWidgetRows(
  private val widgetId: Int,
  private val context: Context,
  private val calendarFutureCase: CalendarFutureCase,
  private val calendarRecentsCase: CalendarRecentsCase,
  private val settingsRepository: SettingsRepository,
) {

  private val imageCorner by lazy { context.dimenToPx(R.dimen.mediaTileCorner) }
  private val imageWidth by lazy { context.dimenToPx(R.dimen.widgetImageWidth) }
  private val imageHeight by lazy { context.dimenToPx(R.dimen.widgetImageHeight) }
  private var mode = PRESENT_FUTURE
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
    val urls = (0 until minOf(upTo, adapterItems.size))
      .mapNotNull { position ->
        val item = adapterItems[position]
        val image = imageOf(item) ?: return@mapNotNull null
        if (image.status != ImageStatus.AVAILABLE) return@mapNotNull null
        idOf(item)?.let { it to image.fullFileUrl }
      }.toMap()
    posters = WidgetPosters.fetch(context, imageWidth, imageHeight, imageCorner, urls)
  }

  private val adapterItems = mutableListOf<CalendarListItem>()

  suspend fun load() {
    palette = WidgetPalettes.resolve(context, widgetId, settingsRepository)
    mode = settingsRepository.widgets.getWidgetCalendarMode(Mode.SHOWS, widgetId)
    val items = when (mode) {
      PRESENT_FUTURE -> calendarFutureCase.loadItems(withFilters = false)
      RECENTS -> calendarRecentsCase.loadItems(withFilters = false)
    }
    adapterItems.replace(items)
  }

  fun viewAt(position: Int) =
    when (val item = adapterItems[position]) {
      is CalendarListItem.Episode -> createItemRemoteView(item)
      is CalendarListItem.Header -> createHeaderRemoteView(item, showIcon = position == 0)
      is CalendarListItem.Filters -> throw IllegalStateException("Filters should not be in the widget")
    }

  private fun createHeaderRemoteView(
    item: CalendarListItem.Header,
    showIcon: Boolean,
  ) = RemoteViews(context.packageName, getHeaderLayout()).apply {
    setTextViewText(R.id.progressWidgetHeaderTitle, context.getString(item.textResId))
    setViewVisibility(R.id.progressWidgetHeaderTitleIcon, if (mode == RECENTS) VISIBLE else GONE)

    if (showIcon) {
      when (mode) {
        PRESENT_FUTURE -> setImageViewResource(R.id.progressWidgetHeaderIcon, R.drawable.ic_history)
        RECENTS -> setImageViewResource(R.id.progressWidgetHeaderIcon, R.drawable.ic_calendar)
      }
      setViewVisibility(R.id.progressWidgetHeaderIcon, VISIBLE)
      val fillIntent = Intent().apply {
        putExtras(bundleOf(EXTRA_MODE_CLICK to true))
        putExtras(bundleOf(EXTRA_APPWIDGET_ID to widgetId))
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

  private fun createItemRemoteView(item: CalendarListItem.Episode): RemoteViews {
    val remoteView = RemoteViews(context.packageName, getItemLayout()).apply {
      val translatedTitle = item.translations?.show?.title
      val title =
        if (translatedTitle?.isBlank() == false) {
          translatedTitle
        } else {
          item.show.title
        }
      setTextViewText(R.id.calendarWidgetItemTitle, title)

      val date = item.episode.firstAired
        ?.toLocalZone()
        ?.let { item.dateFormat?.format(it)?.capitalizeWords() }
      setTextViewText(R.id.calendarWidgetItemDate, date)

      val isNewSeason = item.episode.number == 1
      if (isNewSeason) {
        setTextViewText(
          R.id.calendarWidgetItemOverview,
          String.format(Locale.ENGLISH, context.getString(R.string.textSeason), item.episode.season),
        )
        setTextViewText(R.id.calendarWidgetItemBadge, context.getString(R.string.textNewSeason))
      } else {
        val episodeTitle = when {
          item.episode.title.isBlank() -> context.getString(R.string.textTba)
          item.translations
            ?.episode
            ?.title
            ?.isBlank() == false -> item.translations?.episode?.title
          item.episode.title == "Episode ${item.episode.number}" -> String.format(
            Locale.ENGLISH,
            context.getString(R.string.textEpisode),
            item.episode.number,
          )
          else -> item.episode.title
        }
        val badgeTitle = String
          .format(
            Locale.ENGLISH,
            context.getString(xyz.stignarnia.ui_progress.R.string.textSeasonEpisode),
            item.episode.season,
            item.episode.number,
          ).plus(
            item.episode.numberAbs?.let { if (it > 0 && item.show.isAnime) " ($it)" else "" } ?: "",
          )

        setTextViewText(R.id.calendarWidgetItemOverview, episodeTitle)
        setTextViewText(R.id.calendarWidgetItemBadge, badgeTitle)
      }

      val fillIntent = Intent().apply {
        putExtras(bundleOf(EXTRA_SHOW_ID to item.show.tmdbId))
      }
      setOnClickFillInIntent(R.id.calendarWidgetItem, fillIntent)

      setViewVisibility(R.id.calendarWidgetItemImageBadge, if (item.isWatchlist) VISIBLE else GONE)

      palette?.let {
        setInt(R.id.calendarWidgetItemFrame, "setBackgroundResource", it.mediaFrame)
        setIconTint(R.id.calendarWidgetItemPlaceholder, it.placeholderInk)
        setTextColor(R.id.calendarWidgetItemTitle, it.textPrimary)
        setTextColor(R.id.calendarWidgetItemOverview, it.textPrimary)
        setTextColor(R.id.calendarWidgetItemBadge, it.textPrimary)
        setBackgroundTint(R.id.calendarWidgetItemBadge, it.badge)
        setTextColor(R.id.calendarWidgetItemDate, it.textSecondary)
      }
    }

    if (item.image.status != ImageStatus.AVAILABLE) {
      remoteView.setViewVisibility(R.id.calendarWidgetItemImage, GONE)
      remoteView.setViewVisibility(R.id.calendarWidgetItemPlaceholder, VISIBLE)
      return remoteView
    }

    // The poster, if it has arrived. The counting pass runs before any of them are fetched, so a row without one keeps its placeholder and is filled in by the pass that follows.
    val bitmap = posters[item.show.tmdbId]
    if (bitmap == null) {
      remoteView.setViewVisibility(R.id.calendarWidgetItemImage, GONE)
      remoteView.setViewVisibility(R.id.calendarWidgetItemPlaceholder, VISIBLE)
    } else {
      remoteView.setImageViewBitmap(R.id.calendarWidgetItemImage, bitmap)
      remoteView.setViewVisibility(R.id.calendarWidgetItemImage, VISIBLE)
      remoteView.setViewVisibility(R.id.calendarWidgetItemPlaceholder, GONE)
    }

    return remoteView
  }

  private fun getItemLayout(): Int = R.layout.widget_calendar_item_night

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

  private fun imageOf(item: CalendarListItem) = (item as? CalendarListItem.Episode)?.image

  private fun idOf(item: CalendarListItem) = (item as? CalendarListItem.Episode)?.show?.tmdbId

  fun itemId(position: Int) = adapterItems[position].show.tmdbId

  val count get() = adapterItems.size

  val viewTypeCount = 5

  private companion object {
    const val POSTER_TIMEOUT_SECONDS = 2L
  }
}
