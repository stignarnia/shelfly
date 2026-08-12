package xyz.stignarnia.ui_widgets.progress_movies

import android.content.Context
import android.content.Intent
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
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
import xyz.stignarnia.ui_widgets.R
import xyz.stignarnia.ui_widgets.progress_movies.ProgressMoviesWidgetProvider.Companion.EXTRA_CHECK_MOVIE_ID
import kotlinx.coroutines.runBlocking

class ProgressMoviesWidgetViewsFactory(
  private val context: Context,
  private val loadItemsCase: ProgressMoviesItemsCase,
  private val settingsRepository: SettingsRepository,
) : RemoteViewsService.RemoteViewsFactory {

  private val imageCorner by lazy { context.dimenToPx(R.dimen.mediaTileCorner) }
  private val imageWidth by lazy { context.dimenToPx(R.dimen.widgetImageWidth) }
  private val imageHeight by lazy { context.dimenToPx(R.dimen.widgetImageHeight) }
  private val adapterItems by lazy { mutableListOf<ProgressMovieListItem>() }

  override fun onDataSetChanged() {
    runBlocking {
      val items = loadItemsCase
        .loadItems("")
        .filterIsInstance<ProgressMovieListItem.MovieItem>()
      adapterItems.replace(items)
    }
  }

  override fun getViewAt(position: Int): RemoteViews {
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
        .get()

      remoteView.setImageViewBitmap(R.id.progressMoviesWidgetItemImage, bitmap)
      remoteView.setViewVisibility(R.id.progressMoviesWidgetItemImage, VISIBLE)
    } catch (t: Throwable) {
      remoteView.setViewVisibility(R.id.progressMoviesWidgetItemImage, GONE)
      remoteView.setViewVisibility(R.id.progressMoviesWidgetItemPlaceholder, VISIBLE)
    }

    return remoteView
  }

  private fun getItemLayout(): Int {
    val isLight = settingsRepository.widgets.widgetsTheme == MODE_NIGHT_NO
    return when {
      isLight -> R.layout.widget_movies_progress_item_day
      else -> R.layout.widget_movies_progress_item_night
    }
  }

  override fun getItemId(position: Int) = adapterItems[position].movie.tmdbId

  override fun getLoadingView() = RemoteViews(context.packageName, R.layout.widget_loading_item)

  override fun getCount() = adapterItems.size

  override fun hasStableIds() = true

  override fun getViewTypeCount() = 2

  override fun onCreate() = Unit

  override fun onDestroy() = Unit
}
