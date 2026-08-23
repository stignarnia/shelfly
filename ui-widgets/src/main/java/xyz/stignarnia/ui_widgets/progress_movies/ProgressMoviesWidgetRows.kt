package xyz.stignarnia.ui_widgets.progress_movies

import android.content.Context
import android.content.Intent
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RemoteViews
import androidx.core.os.bundleOf
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_base.utilities.extensions.dimenToPx
import xyz.stignarnia.ui_base.utilities.extensions.replace
import xyz.stignarnia.ui_model.ImageStatus
import xyz.stignarnia.ui_progress_movies.progress.cases.ProgressMoviesItemsCase
import xyz.stignarnia.ui_progress_movies.progress.recycler.ProgressMovieListItem
import xyz.stignarnia.ui_widgets.BaseWidgetProvider.Companion.EXTRA_MOVIE_ID
import xyz.stignarnia.ui_widgets.BaseWidgetProvider
import xyz.stignarnia.ui_widgets.R
import xyz.stignarnia.ui_widgets.progress_movies.ProgressMoviesWidgetProvider.Companion.EXTRA_CHECK_MOVIE_ID
import xyz.stignarnia.ui_widgets.theme.WidgetPalette
import xyz.stignarnia.ui_widgets.theme.WidgetPalettes
import xyz.stignarnia.ui_widgets.theme.setIconTint
import java.util.concurrent.TimeUnit

/**
 * The rows of the shows movies progress widget, built to be carried inside the widget's own views.
 *
 * Was a RemoteViewsFactory behind a RemoteViewsService, which the launcher called back into per row.
 * The framework never delivers views that name such a service - see WidgetCollection - so the rows are built here instead and travel with the frame.
 */
class ProgressMoviesWidgetRows(
  private val widgetId: Int,
  private val context: Context,
  private val loadItemsCase: ProgressMoviesItemsCase,
  private val settingsRepository: SettingsRepository,
) {

  private val imageCorner by lazy { context.dimenToPx(R.dimen.mediaTileCorner) }
  private val imageWidth by lazy { context.dimenToPx(R.dimen.widgetImageWidth) }
  private val imageHeight by lazy { context.dimenToPx(R.dimen.widgetImageHeight) }
  private val adapterItems by lazy { mutableListOf<ProgressMovieListItem>() }
  private var palette: WidgetPalette? = null

  suspend fun load() {
    palette = WidgetPalettes.resolve(context, widgetId, settingsRepository)
    val items = loadItemsCase
      .loadItems("")
      .filterIsInstance<ProgressMovieListItem.MovieItem>()
    adapterItems.replace(items)
  }

  fun viewAt(position: Int): RemoteViews {
    val item = adapterItems[position] as ProgressMovieListItem.MovieItem
    return createItemRemoteView(item)
  }

  private fun createItemRemoteView(item: ProgressMovieListItem.MovieItem): RemoteViews {
    val translatedTitle = item.translation?.title
    val title =
      if (translatedTitle?.isBlank() == false) {
        translatedTitle
      } else {
        item.movie.title
      }

    val translatedDescription = item.translation?.overview
    val description =
      if (translatedDescription?.isBlank() == false) {
        translatedDescription
      } else {
        item.movie.overview
      }

    val remoteView = RemoteViews(context.packageName, getItemLayout()).apply {
      setTextViewText(R.id.progressMoviesWidgetItemTitle, title)
      setTextViewText(R.id.progressMoviesWidgetItemSubtitle2, description)

      val fillIntent = Intent().apply {
        putExtras(bundleOf(EXTRA_MOVIE_ID to item.movie.tmdbId))
      }
      setOnClickFillInIntent(R.id.progressMoviesWidgetItem, fillIntent)

      val checkFillIntent = Intent().apply {
        putExtras(bundleOf(EXTRA_CHECK_MOVIE_ID to item.movie.tmdbId))
      }
      setOnClickFillInIntent(R.id.progressMoviesWidgetItemCheckButton, checkFillIntent)

      palette?.let {
        setInt(R.id.progressMoviesWidgetItemFrame, "setBackgroundResource", it.mediaFrame)
        setIconTint(R.id.progressMoviesWidgetItemPlaceholder, it.placeholderInk)
        setTextColor(R.id.progressMoviesWidgetItemTitle, it.textPrimary)
        setTextColor(R.id.progressMoviesWidgetItemSubtitle2, it.textSecondary)
        setIconTint(R.id.progressMoviesWidgetItemCheckButton, it.textPrimary)
      }
    }

    if (item.image.status != ImageStatus.AVAILABLE) {
      remoteView.setViewVisibility(R.id.progressMoviesWidgetItemImage, GONE)
      remoteView.setViewVisibility(R.id.progressMoviesWidgetItemPlaceholder, VISIBLE)
      return remoteView
    }

    try {
      remoteView.setViewVisibility(R.id.progressMoviesWidgetItemImage, GONE)
      remoteView.setViewVisibility(R.id.progressMoviesWidgetItemPlaceholder, GONE)

      val bitmap = Glide
        .with(context)
        .asBitmap()
        .load(item.image.fullFileUrl)
        .transform(CenterCrop(), RoundedCorners(imageCorner))
        .submit(imageWidth, imageHeight)
        // Time boxed: every row's poster is fetched before the widget can be sent, so one slow image must not hold the whole list up.
        // A miss falls through to the placeholder and is picked up on the next update, by which point Glide has it cached.
        .get(POSTER_TIMEOUT_SECONDS, TimeUnit.SECONDS)

      remoteView.setImageViewBitmap(R.id.progressMoviesWidgetItemImage, bitmap)
      remoteView.setViewVisibility(R.id.progressMoviesWidgetItemImage, VISIBLE)
    } catch (t: Throwable) {
      remoteView.setViewVisibility(R.id.progressMoviesWidgetItemImage, GONE)
      remoteView.setViewVisibility(R.id.progressMoviesWidgetItemPlaceholder, VISIBLE)
    }

    return remoteView
  }

  private fun getItemLayout(): Int = R.layout.widget_movies_progress_item_night

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

  val viewTypeCount = 3

  private companion object {
    const val POSTER_TIMEOUT_SECONDS = 2L
  }
}
