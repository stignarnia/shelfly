package xyz.stignarnia.uiStatistics.views.ratings

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import com.bumptech.glide.Glide
import xyz.stignarnia.uiBase.common.views.ShowView
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.gone
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiStatistics.R
import xyz.stignarnia.uiStatistics.databinding.ViewStatisticsRateItemBinding
import xyz.stignarnia.uiStatistics.views.ratings.recycler.StatisticsRatingItem

class StatisticsRateItemView : ShowView<StatisticsRatingItem> {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewStatisticsRateItemBinding.inflate(LayoutInflater.from(context), this)

  init {
    val width = context.dimenToPx(R.dimen.statisticsRatingItemWidth)
    layoutParams = LayoutParams(width, WRAP_CONTENT)
    clipChildren = false
    binding.viewRateItemImageLayout.onClick { itemClickListener?.invoke(item) }
  }

  override val imageView: ImageView = binding.viewRateItemImage
  override val placeholderView: ImageView = binding.viewRateItemPlaceholder

  private lateinit var item: StatisticsRatingItem

  override fun bind(item: StatisticsRatingItem) {
    this.item = item
    clear()
    with(binding) {
      viewRateItemTitle.text = item.show.title
      viewRateItemRating.text = "${item.rating.rating}"
    }
    loadImage(item)
  }

  private fun clear() {
    with(binding) {
      viewRateItemTitle.gone()
      viewRateItemPlaceholder.gone()
      Glide.with(this@StatisticsRateItemView).clear(viewRateItemImage)
    }
  }
}
