package xyz.stignarnia.uiDiscoverMovies.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import com.bumptech.glide.Glide
import xyz.stignarnia.uiBase.common.views.MovieView
import xyz.stignarnia.uiBase.utilities.extensions.gone
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.onLongClick
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiDiscoverMovies.R
import xyz.stignarnia.uiDiscoverMovies.databinding.ViewMovieFanartBinding
import xyz.stignarnia.uiDiscoverMovies.recycler.DiscoverMovieListItem
import xyz.stignarnia.uiModel.ImageStatus.AVAILABLE
import xyz.stignarnia.uiModel.ImageStatus.UNAVAILABLE

class MovieFanartView : MovieView<DiscoverMovieListItem> {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewMovieFanartBinding.inflate(LayoutInflater.from(context), this)

  init {
    with(binding.movieFanartRoot) {
      onClick { itemClickListener?.invoke(item) }
      onLongClick { itemLongClickListener?.invoke(item) }
    }
  }

  override val imageView: ImageView = binding.movieFanartImage
  override val placeholderView: ImageView = binding.movieFanartPlaceholder

  private lateinit var item: DiscoverMovieListItem

  override fun bind(item: DiscoverMovieListItem) {
    super.bind(item)
    clear()
    this.item = item
    with(binding) {
      movieFanartTitle.text =
        if (item.translation?.title.isNullOrBlank()) {
          item.movie.title
        } else {
          item.translation.title
        }
      movieFanartProgress.visibleIf(item.isLoading)
      movieFanartBadge.visibleIf(item.isCollected)
      movieFanartBadgeLater.visibleIf(item.isWatchlist)
    }
    loadImage(item)
  }

  override fun loadImage(item: DiscoverMovieListItem) {
    super.loadImage(item)
    if (item.image.status == UNAVAILABLE) {
      binding.movieFanartRoot.setBackgroundResource(R.drawable.bg_media_view_placeholder)
    }
  }

  override fun onImageLoadFail(item: DiscoverMovieListItem) {
    super.onImageLoadFail(item)
    if (item.image.status == AVAILABLE) {
      binding.movieFanartRoot.setBackgroundResource(R.drawable.bg_media_view_placeholder)
    }
  }

  private fun clear() {
    with(binding) {
      movieFanartTitle.text = ""
      movieFanartProgress.gone()
      movieFanartPlaceholder.gone()
      movieFanartRoot.setBackgroundResource(R.drawable.bg_media_view_elevation)
      movieFanartBadge.gone()
      Glide.with(this@MovieFanartView).clear(movieFanartImage)
    }
  }
}
