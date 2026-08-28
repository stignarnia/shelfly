package xyz.stignarnia.uiWidgets.progressMovies

import android.content.Context
import android.content.Intent
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.replace
import xyz.stignarnia.uiModel.ImageStatus
import xyz.stignarnia.uiProgressMovies.progress.cases.ProgressMoviesItemsCase
import xyz.stignarnia.uiProgressMovies.progress.recycler.ProgressMovieListItem
import xyz.stignarnia.uiWidgets.BaseWidgetProvider
import xyz.stignarnia.uiWidgets.BaseWidgetProvider.Companion.EXTRA_MOVIE_ID
import xyz.stignarnia.uiWidgets.R
import xyz.stignarnia.uiWidgets.progressMovies.ProgressMoviesWidgetProvider.Companion.EXTRA_CHECK_MOVIE_ID
import xyz.stignarnia.uiWidgets.theme.WidgetPalette
import xyz.stignarnia.uiWidgets.theme.WidgetPalettes
import xyz.stignarnia.uiWidgets.theme.WidgetPosters
import xyz.stignarnia.uiWidgets.theme.setIconTint
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

  suspend fun load() {
    palette = WidgetPalettes.resolve(context, widgetId, settingsRepository)
    val items =
      loadItemsCase
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

    val remoteView =
      RemoteViews(context.packageName, getItemLayout()).apply {
        setTextViewText(R.id.progressMoviesWidgetItemTitle, title)
        setTextViewText(R.id.progressMoviesWidgetItemSubtitle2, description)

        val fillIntent =
          Intent().apply {
            putExtra(EXTRA_MOVIE_ID, item.movie.tmdbId)
          }
        setOnClickFillInIntent(R.id.progressMoviesWidgetItem, fillIntent)

        val checkFillIntent =
          Intent().apply {
            putExtra(EXTRA_CHECK_MOVIE_ID, item.movie.tmdbId)
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

    // The poster, if it has arrived. The counting pass runs before any of them are fetched, so a row without one keeps its placeholder and is filled in by the pass that follows.
    val bitmap = posters[item.movie.tmdbId]
    if (bitmap == null) {
      remoteView.setViewVisibility(R.id.progressMoviesWidgetItemImage, GONE)
      remoteView.setViewVisibility(R.id.progressMoviesWidgetItemPlaceholder, VISIBLE)
    } else {
      remoteView.setImageViewBitmap(R.id.progressMoviesWidgetItemImage, bitmap)
      remoteView.setViewVisibility(R.id.progressMoviesWidgetItemImage, VISIBLE)
      remoteView.setViewVisibility(R.id.progressMoviesWidgetItemPlaceholder, GONE)
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
        Intent().putExtra(BaseWidgetProvider.EXTRA_MORE_CLICK, true),
      )
    }

  private fun imageOf(item: ProgressMovieListItem) = (item as? ProgressMovieListItem.MovieItem)?.image

  private fun idOf(item: ProgressMovieListItem) = (item as? ProgressMovieListItem.MovieItem)?.movie?.tmdbId

  fun itemId(position: Int) = adapterItems[position].movie.tmdbId

  val count get() = adapterItems.size

  val viewTypeCount = 3

  private companion object {
    const val POSTER_TIMEOUT_SECONDS = 2L
  }
}
