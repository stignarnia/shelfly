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
import xyz.stignarnia.uiBase.utilities.extensions.visible
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiDiscoverMovies.R
import xyz.stignarnia.uiDiscoverMovies.databinding.ViewMoviePosterBinding
import xyz.stignarnia.uiDiscoverMovies.recycler.DiscoverMovieListItem
import xyz.stignarnia.uiModel.ImageStatus.AVAILABLE
import xyz.stignarnia.uiModel.ImageStatus.UNAVAILABLE

class MoviePosterView : MovieView<DiscoverMovieListItem> {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewMoviePosterBinding.inflate(LayoutInflater.from(context), this)

  init {
    with(binding.moviePosterRoot) {
      onClick { itemClickListener?.invoke(item) }
      onLongClick { itemLongClickListener?.invoke(item) }
    }
  }

  override val imageView: ImageView = binding.moviePosterImage
  override val placeholderView: ImageView = binding.moviePosterPlaceholder

  private lateinit var item: DiscoverMovieListItem

  override fun bind(item: DiscoverMovieListItem) {
    super.bind(item)
    clear()
    this.item = item
    with(binding) {
      moviePosterTitle.text = item.movie.title
      moviePosterProgress.visibleIf(item.isLoading)
      moviePosterBadge.visibleIf(item.isCollected)
      moviePosterLaterBadge.visibleIf(item.isWatchlist)
    }
    loadImage(item)
  }

  override fun loadImage(item: DiscoverMovieListItem) {
    if (item.image.status == UNAVAILABLE) {
      with(binding) {
        moviePosterTitle.visible()
        moviePosterRoot.setBackgroundResource(R.drawable.bg_media_view_placeholder)
      }
    }
    super.loadImage(item)
  }

  override fun onImageLoadFail(item: DiscoverMovieListItem) {
    super.onImageLoadFail(item)
    if (item.image.status == AVAILABLE) {
      with(binding) {
        moviePosterTitle.visible()
        moviePosterRoot.setBackgroundResource(R.drawable.bg_media_view_placeholder)
      }
    }
  }

  private fun clear() {
    with(binding) {
      moviePosterTitle.text = ""
      moviePosterTitle.gone()
      moviePosterRoot.setBackgroundResource(R.drawable.bg_media_view_elevation)
      moviePosterPlaceholder.gone()
      moviePosterProgress.gone()
      moviePosterBadge.gone()
      Glide.with(this@MoviePosterView).clear(moviePosterImage)
    }
  }
}
