package xyz.stignarnia.ui_widgets.progress

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RemoteViews
import androidx.core.os.bundleOf
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_base.utilities.DurationPrinter
import xyz.stignarnia.ui_base.utilities.extensions.dimenToPx
import xyz.stignarnia.ui_base.utilities.extensions.replace
import xyz.stignarnia.ui_model.ImageStatus
import xyz.stignarnia.ui_model.Season
import xyz.stignarnia.ui_progress.progress.cases.ProgressItemsCase
import xyz.stignarnia.ui_progress.progress.recycler.ProgressListItem
import xyz.stignarnia.ui_widgets.BaseWidgetProvider.Companion.EXTRA_SHOW_ID
import xyz.stignarnia.ui_widgets.BaseWidgetProvider
import xyz.stignarnia.ui_widgets.R
import xyz.stignarnia.ui_widgets.progress.ProgressWidgetProvider.Companion.EXTRA_EPISODE_ID
import xyz.stignarnia.ui_widgets.progress.ProgressWidgetProvider.Companion.EXTRA_SEASON_ID
import xyz.stignarnia.ui_widgets.theme.WidgetPalette
import xyz.stignarnia.ui_widgets.theme.WidgetPalettes
import xyz.stignarnia.ui_widgets.theme.setBackgroundTint
import xyz.stignarnia.ui_widgets.theme.setIconTint
import xyz.stignarnia.ui_widgets.theme.setProgressTint
import java.util.Locale.ENGLISH
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * The rows of the shows progress widget, built to be carried inside the widget's own views.
 *
 * Was a RemoteViewsFactory behind a RemoteViewsService, which the launcher called back into per row.
 * The framework never delivers views that name such a service - see WidgetCollection - so the rows are built here instead and travel with the frame.
 */
class ProgressWidgetRows(
  private val widgetId: Int,
  private val context: Context,
  private val itemsCase: ProgressItemsCase,
  private val settingsRepository: SettingsRepository,
) {

  private val imageCorner by lazy { context.dimenToPx(R.dimen.mediaTileCorner) }
  private val imageWidth by lazy { context.dimenToPx(R.dimen.widgetImageWidth) }
  private val imageHeight by lazy { context.dimenToPx(R.dimen.widgetImageHeight) }
  private val checkWidth by lazy { context.dimenToPx(R.dimen.widgetCheckButtonWidth) }
  private val spaceMedium by lazy { context.dimenToPx(R.dimen.spaceMedium) }
  private val adapterItems by lazy { mutableListOf<ProgressListItem>() }
  private val durationPrinter by lazy { DurationPrinter(context.applicationContext) }

  /**
   * Re-read on every refresh rather than held from construction: a theme picked in the launcher changes nothing about the data, and notifyAppWidgetViewDataChanged is the only thing that runs afterwards.
   */
  private var palette: WidgetPalette? = null

  suspend fun load() {
    palette = WidgetPalettes.resolve(context, widgetId, settingsRepository)
    val items = itemsCase
      .loadWidgetItems()
      .filterNot { it is ProgressListItem.Filters }
    adapterItems.replace(items)
  }

  fun viewAt(position: Int) =
    when (val item = adapterItems[position]) {
      is ProgressListItem.Episode -> createItemRemoteView(item)
      is ProgressListItem.Header -> createHeaderRemoteView(item)
      else -> throw IllegalStateException()
    }

  private fun createItemRemoteView(item: ProgressListItem.Episode): RemoteViews {
    val title =
      if (item.translations
          ?.show
          ?.title
          ?.isBlank() == false
      ) {
        item.translations?.show?.title
      } else {
        item.show.title
      }

    val subtitle = String
      .format(ENGLISH, "S.%02d E.%02d", item.episode?.season, item.episode?.number)
      .plus(item.episode?.numberAbs?.let { if (it > 0 && item.show.isAnime) " ($it)" else "" } ?: "")

    var percent = 0
    if (item.totalCount != 0) {
      percent = ((item.watchedCount.toFloat() / item.totalCount.toFloat()) * 100F).roundToInt()
    }
    val progressText =
      String.format(ENGLISH, "%d/%d (%d%%)", item.watchedCount, item.totalCount, percent)
    val imageUrl = item.image.fullFileUrl
    val hasAired = item.episode?.hasAired(item.season ?: Season.EMPTY) == true
    val subtitle2 = when {
      item.episode?.title?.isBlank() == true -> {
        context.getString(R.string.textTba)
      }
      item.translations
        ?.episode
        ?.title
        ?.isBlank() == false -> {
        item.translations?.episode?.title ?: context.getString(R.string.textTba)
      }
      item.episode?.title == "Episode ${item.episode?.number}" -> {
        String.format(ENGLISH, context.getString(R.string.textEpisode), item.episode?.number)
      }
      else -> {
        item.episode?.title
      }
    }

    val remoteView = RemoteViews(context.packageName, getItemLayout()).apply {
      setTextViewText(R.id.progressWidgetItemTitle, title)
      setTextViewText(R.id.progressWidgetItemSubtitle, subtitle)
      setTextViewText(R.id.progressWidgetItemSubtitle2, subtitle2)
      setTextViewText(R.id.progressWidgetItemProgressText, progressText)
      setViewVisibility(R.id.progressWidgetItemBadge, if (item.isNew()) VISIBLE else GONE)
      setProgressBar(R.id.progressWidgetItemProgress, item.totalCount, item.watchedCount, false)
      if (hasAired) {
        setViewVisibility(R.id.progressWidgetItemCheckButton, VISIBLE)
        setViewVisibility(R.id.progressWidgetItemDateButton, GONE)
        setViewPadding(R.id.progressWidgetItemProgress, 0, 0, checkWidth, 0)
      } else {
        setViewVisibility(R.id.progressWidgetItemCheckButton, GONE)
        setViewVisibility(R.id.progressWidgetItemDateButton, VISIBLE)
        setTextViewText(R.id.progressWidgetItemDateButton, durationPrinter.print(item.episode?.firstAired))
        setViewPadding(R.id.progressWidgetItemProgress, 0, 0, spaceMedium, 0)
      }

      val fillIntent = Intent().apply {
        putExtras(
          Bundle().apply {
            putExtra(EXTRA_SHOW_ID, item.show.tmdbId)
          },
        )
      }
      setOnClickFillInIntent(R.id.progressWidgetItem, fillIntent)

      val checkFillIntent = Intent().apply {
        putExtras(
          Bundle().apply {
            putExtra(
              EXTRA_EPISODE_ID,
              item.episode
                ?.ids
                ?.tmdb
                ?.id,
            )
            putExtra(
              EXTRA_SEASON_ID,
              item.season
                ?.ids
                ?.tmdb
                ?.id,
            )
            putExtra(EXTRA_SHOW_ID, item.show.tmdbId)
          },
        )
      }
      setOnClickFillInIntent(R.id.progressWidgetItemCheckButton, checkFillIntent)

      palette?.let {
        setInt(R.id.progressWidgetItemFrame, "setBackgroundResource", it.mediaFrame)
        setIconTint(R.id.progressWidgetItemPlaceholder, it.placeholderInk)
        setTextColor(R.id.progressWidgetItemTitle, it.textPrimary)
        setTextColor(R.id.progressWidgetItemBadge, it.accentText)
        setTextColor(R.id.progressWidgetItemSubtitle, it.textPrimary)
        setBackgroundTint(R.id.progressWidgetItemSubtitle, it.badge)
        setTextColor(R.id.progressWidgetItemSubtitle2, it.textPrimary)
        setTextColor(R.id.progressWidgetItemProgressText, it.textSecondary)
        setProgressTint(R.id.progressWidgetItemProgress, it.accent, it.textSecondary)
        setIconTint(R.id.progressWidgetItemCheckButton, it.textPrimary)
        setTextColor(R.id.progressWidgetItemDateButton, it.textSecondary)
      }
    }

    if (item.image.status != ImageStatus.AVAILABLE) {
      remoteView.setViewVisibility(R.id.progressWidgetItemImage, GONE)
      remoteView.setViewVisibility(R.id.progressWidgetItemPlaceholder, VISIBLE)
      return remoteView
    }

    try {
      remoteView.setViewVisibility(R.id.progressWidgetItemImage, GONE)
      remoteView.setViewVisibility(R.id.progressWidgetItemPlaceholder, GONE)

      val bitmap = Glide
        .with(context)
        .asBitmap()
        .load(imageUrl)
        .transform(CenterCrop(), RoundedCorners(imageCorner))
        .submit(imageWidth, imageHeight)
        // Time boxed: every row's poster is fetched before the widget can be sent, so one slow image must not hold the whole list up.
        // A miss falls through to the placeholder and is picked up on the next update, by which point Glide has it cached.
        .get(POSTER_TIMEOUT_SECONDS, TimeUnit.SECONDS)

      remoteView.setImageViewBitmap(R.id.progressWidgetItemImage, bitmap)
      remoteView.setViewVisibility(R.id.progressWidgetItemImage, VISIBLE)
    } catch (t: Throwable) {
      remoteView.setViewVisibility(R.id.progressWidgetItemImage, GONE)
      remoteView.setViewVisibility(R.id.progressWidgetItemPlaceholder, VISIBLE)
    }

    return remoteView
  }

  private fun createHeaderRemoteView(item: ProgressListItem.Header) =
    RemoteViews(context.packageName, getHeaderLayout()).apply {
      setTextViewText(R.id.progressWidgetHeaderTitle, context.getString(item.textResId))
      palette?.let {
        setTextColor(R.id.progressWidgetHeaderTitle, it.textPrimary)
        setIconTint(R.id.progressWidgetHeaderTitleIcon, it.textPrimary)
        setIconTint(R.id.progressWidgetHeaderIcon, it.textPrimary)
      }
    }

  private fun getItemLayout(): Int = R.layout.widget_progress_item_night

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

  fun itemId(position: Int) = adapterItems[position].show.tmdbId

  val count get() = adapterItems.size

  val viewTypeCount = 5

  private companion object {
    const val POSTER_TIMEOUT_SECONDS = 2L
  }
}
