package xyz.stignarnia.uiStatisticsMovies.views.ratings

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import com.bumptech.glide.Glide
import xyz.stignarnia.uiBase.common.views.MovieView
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.gone
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiStatisticsMovies.R
import xyz.stignarnia.uiStatisticsMovies.databinding.ViewStatisticsMoviesRateItemBinding
import xyz.stignarnia.uiStatisticsMovies.views.ratings.recycler.StatisticsMoviesRatingItem

class StatisticsMoviesRateItemView : MovieView<StatisticsMoviesRatingItem> {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewStatisticsMoviesRateItemBinding.inflate(LayoutInflater.from(context), this)

  init {
    val width = context.dimenToPx(R.dimen.statisticsMoviesRatingItemWidth)
    layoutParams = LayoutParams(width, WRAP_CONTENT)
    clipChildren = false
    binding.viewMovieRateItemImageLayout.onClick { itemClickListener?.invoke(item) }
  }

  override val imageView: ImageView = binding.viewMovieRateItemImage
  override val placeholderView: ImageView = binding.viewMovieRateItemPlaceholder

  private lateinit var item: StatisticsMoviesRatingItem

  override fun bind(item: StatisticsMoviesRatingItem) {
    this.item = item
    clear()
    with(binding) {
      viewMovieRateItemTitle.text = item.movie.title
      viewMovieRateItemRating.text = "${item.rating.rating}"
    }
    loadImage(item)
  }

  private fun clear() {
    with(binding) {
      viewMovieRateItemTitle.gone()
      viewMovieRateItemPlaceholder.gone()
      Glide.with(this@StatisticsMoviesRateItemView).clear(viewMovieRateItemImage)
    }
  }
}
