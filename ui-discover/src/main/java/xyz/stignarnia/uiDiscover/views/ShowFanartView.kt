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
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiDiscover.R
import xyz.stignarnia.uiDiscover.databinding.ViewShowFanartBinding
import xyz.stignarnia.uiDiscover.recycler.DiscoverListItem
import xyz.stignarnia.uiModel.ImageStatus.AVAILABLE
import xyz.stignarnia.uiModel.ImageStatus.UNAVAILABLE

class ShowFanartView : ShowView<DiscoverListItem> {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewShowFanartBinding.inflate(LayoutInflater.from(context), this)

  init {
    with(binding) {
      showFanartRoot.onClick { itemClickListener?.invoke(item) }
      showFanartRoot.onLongClick { itemLongClickListener?.invoke(item) }
    }
  }

  override val imageView: ImageView = binding.showFanartImage
  override val placeholderView: ImageView = binding.showFanartPlaceholder

  private lateinit var item: DiscoverListItem

  override fun bind(item: DiscoverListItem) {
    super.bind(item)
    clear()
    this.item = item
    with(binding) {
      showFanartTitle.text =
        if (item.translation?.title.isNullOrBlank()) {
          item.show.title
        } else {
          item.translation.title
        }
      showFanartProgress.visibleIf(item.isLoading)
      showFanartBadge.visibleIf(item.isFollowed)
      showFanartBadgeLater.visibleIf(item.isWatchlist)
    }
    loadImage(item)
  }

  override fun loadImage(item: DiscoverListItem) {
    super.loadImage(item)
    if (item.image.status == UNAVAILABLE) {
      binding.showFanartRoot.setBackgroundResource(R.drawable.bg_media_view_placeholder)
    }
  }

  override fun onImageLoadFail(item: DiscoverListItem) {
    super.onImageLoadFail(item)
    if (item.image.status == AVAILABLE) {
      binding.showFanartRoot.setBackgroundResource(R.drawable.bg_media_view_placeholder)
    }
  }

  private fun clear() {
    with(binding) {
      showFanartTitle.text = ""
      showFanartProgress.gone()
      showFanartPlaceholder.gone()
      showFanartRoot.setBackgroundResource(R.drawable.bg_media_view_elevation)
      showFanartBadge.gone()
      Glide.with(this@ShowFanartView).clear(showFanartImage)
    }
  }
}
