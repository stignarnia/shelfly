package xyz.stignarnia.uiBase.common.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import xyz.stignarnia.common.Config.IMAGE_FADE_DURATION_MS
import xyz.stignarnia.common.Config.MAIN_GRID_SPAN
import xyz.stignarnia.common.Config.MAIN_GRID_SPAN_TABLET
import xyz.stignarnia.uiBase.R
import xyz.stignarnia.uiBase.common.MovieListItem
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.isTablet
import xyz.stignarnia.uiBase.utilities.extensions.visible
import xyz.stignarnia.uiBase.utilities.extensions.withFailListener
import xyz.stignarnia.uiBase.utilities.extensions.withSuccessListener
import xyz.stignarnia.uiModel.ImageStatus.AVAILABLE
import xyz.stignarnia.uiModel.ImageStatus.UNAVAILABLE
import xyz.stignarnia.uiModel.ImageStatus.UNKNOWN

abstract class MovieView<Item : MovieListItem> : FrameLayout {
  companion object {
    const val ASPECT_RATIO = 1.4705
  }

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val cornerRadius by lazy { context.dimenToPx(R.dimen.mediaTileCorner) }
  private val gridPadding by lazy { context.dimenToPx(R.dimen.gridPadding) }
  private val centerCropTransformation by lazy { CenterCrop() }
  private val cornersTransformation by lazy { RoundedCorners(cornerRadius) }

  private val isTablet by lazy { context.isTablet() }
  private var currentSpan = 1
  private var hasFixedAspectRatio = false

  protected abstract val imageView: ImageView
  protected abstract val placeholderView: ImageView

  var itemClickListener: ((Item) -> Unit)? = null
  var itemLongClickListener: ((Item) -> Unit)? = null
  var imageLoadCompleteListener: (() -> Unit)? = null
  var missingImageListener: ((Item, Boolean) -> Unit)? = null
  var missingTranslationListener: ((Item) -> Unit)? = null

  open fun bind(item: Item) {
    hasFixedAspectRatio = true
    currentSpan =
      item.image.type
        .getSpan(isTablet)
        .coerceAtLeast(1)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    if (hasFixedAspectRatio && currentSpan > 0) {
      val width = MeasureSpec.getSize(widthMeasureSpec)
      if (width > 0) {
        val singleSpanWidth = width.toFloat() / currentSpan
        val height = (singleSpanWidth * ASPECT_RATIO).toInt()
        super.onMeasure(
          widthMeasureSpec,
          MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
        )
        return
      }
    }
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
  }

  protected open fun loadImage(item: Item) {
    if (item.isLoading) return

    if (item.image.status == UNAVAILABLE) {
      placeholderView.visible()
      return
    }

    if (item.image.status == UNKNOWN) {
      onImageLoadFail(item)
      return
    }

    Glide
      .with(this)
      .load(item.image.fullFileUrl)
      .transform(centerCropTransformation, cornersTransformation)
      .transition(withCrossFade(IMAGE_FADE_DURATION_MS))
      .withSuccessListener { onImageLoadSuccess() }
      .withFailListener { onImageLoadFail(item) }
      .into(imageView)
  }

  protected open fun onImageLoadSuccess() = imageLoadCompleteListener?.invoke()

  protected open fun onImageLoadFail(item: Item) {
    if (item.image.status == AVAILABLE) {
      placeholderView.visible()
      imageLoadCompleteListener?.invoke()
      return
    }
    val force = (item.image.status == UNKNOWN)
    missingImageListener?.invoke(item, force)
  }
}
