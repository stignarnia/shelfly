package xyz.stignarnia.uiDiscover.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import com.bumptech.glide.Glide
import xyz.stignarnia.uiBase.common.views.ShowView
import xyz.stignarnia.uiBase.utilities.extensions.gone
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.onLongClick
import xyz.stignarnia.uiBase.utilities.extensions.visible
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiDiscover.R
import xyz.stignarnia.uiDiscover.databinding.ViewShowPosterBinding
import xyz.stignarnia.uiDiscover.recycler.DiscoverListItem
import xyz.stignarnia.uiModel.ImageStatus.AVAILABLE
import xyz.stignarnia.uiModel.ImageStatus.UNAVAILABLE

class ShowPosterView : ShowView<DiscoverListItem> {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewShowPosterBinding.inflate(LayoutInflater.from(context), this)

  init {
    with(binding) {
      showPosterRoot.onClick { itemClickListener?.invoke(item) }
      showPosterRoot.onLongClick { itemLongClickListener?.invoke(item) }
    }
  }

  override val imageView: ImageView = binding.showPosterImage
  override val placeholderView: ImageView = binding.showPosterPlaceholder

  private lateinit var item: DiscoverListItem

  override fun bind(item: DiscoverListItem) {
    super.bind(item)
    clear()
    this.item = item

    with(binding) {
      showPosterTitle.text = item.show.title
      showPosterProgress.visibleIf(item.isLoading)
      showPosterBadge.visibleIf(item.isFollowed)
      showPosterLaterBadge.visibleIf(item.isWatchlist)
    }

    loadImage(item)
  }

  override fun loadImage(item: DiscoverListItem) {
    if (item.image.status == UNAVAILABLE) {
      with(binding) {
        showPosterTitle.visible()
        showPosterRoot.setBackgroundResource(R.drawable.bg_media_view_placeholder)
      }
    }
    super.loadImage(item)
  }

  override fun onImageLoadFail(item: DiscoverListItem) {
    super.onImageLoadFail(item)
    if (item.image.status == AVAILABLE) {
      with(binding) {
        showPosterTitle.visible()
        showPosterRoot.setBackgroundResource(R.drawable.bg_media_view_placeholder)
      }
    }
  }

  private fun clear() {
    with(binding) {
      showPosterTitle.text = ""
      showPosterTitle.gone()
      showPosterRoot.setBackgroundResource(R.drawable.bg_media_view_elevation)
      showPosterPlaceholder.gone()
      showPosterProgress.gone()
      showPosterBadge.gone()
      Glide.with(this@ShowPosterView).clear(showPosterImage)
    }
  }
}
