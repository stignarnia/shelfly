package xyz.stignarnia.uiMyMovies.mymovies.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import xyz.stignarnia.common.Config.IMAGE_FADE_DURATION_MS
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.gone
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.onLongClick
import xyz.stignarnia.uiBase.utilities.extensions.visible
import xyz.stignarnia.uiBase.utilities.extensions.withFailListener
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.ImageStatus
import xyz.stignarnia.uiMyMovies.R
import xyz.stignarnia.uiMyMovies.databinding.ViewMyMoviesFanartBinding
import xyz.stignarnia.uiMyMovies.mymovies.recycler.MyMoviesItem

class MyMovieFanartView : FrameLayout {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewMyMoviesFanartBinding.inflate(LayoutInflater.from(context), this)

  init {
    setBackgroundResource(R.drawable.bg_media_view_elevation)
    elevation = context.dimenToPx(R.dimen.elevationSmall).toFloat()
  }

  private val cornerRadius by lazy { context.dimenToPx(R.dimen.myMoviesFanartCorner) }

  fun bind(
    item: MyMoviesItem,
    clickListener: (MyMoviesItem) -> Unit,
    longClickListener: (MyMoviesItem) -> Unit,
  ) {
    clear()
    with(binding) {
      myMovieFanartTitle.visible()
      myMovieFanartTitle.text =
        if (item.translation?.title.isNullOrBlank()) {
          item.movie.title
        } else {
          item.translation.title
        }
    }
    onClick { clickListener(item) }
    onLongClick { longClickListener(item) }
    loadImage(item.image)
  }

  private fun loadImage(image: Image) {
    with(binding) {
      if (image.status != ImageStatus.AVAILABLE) {
        myMovieFanartPlaceholder.visible()
        myMovieFanartRoot.setBackgroundResource(R.drawable.bg_media_view_placeholder)
        return
      }
      Glide
        .with(this@MyMovieFanartView)
        .load(image.fullFileUrl)
        .transform(CenterCrop(), RoundedCorners(cornerRadius))
        .transition(DrawableTransitionOptions.withCrossFade(IMAGE_FADE_DURATION_MS))
        .withFailListener {
          myMovieFanartPlaceholder.visible()
          myMovieFanartImage.gone()
        }.into(myMovieFanartImage)
    }
  }

  private fun clear() {
    with(binding) {
      myMovieFanartPlaceholder.gone()
      myMovieFanartTitle.text = ""
      myMovieFanartRoot.setBackgroundResource(0)
      Glide.with(this@MyMovieFanartView).clear(myMovieFanartImage)
    }
  }
}
