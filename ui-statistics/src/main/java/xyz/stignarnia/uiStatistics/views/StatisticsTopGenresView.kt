package xyz.stignarnia.uiStatistics.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.google.android.material.card.MaterialCardView
import xyz.stignarnia.uiBase.utilities.extensions.colorFromAttr
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiModel.Genre
import xyz.stignarnia.uiStatistics.R
import xyz.stignarnia.uiStatistics.databinding.ViewStatisticsCardTopGenreBinding

class StatisticsTopGenresView : MaterialCardView {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewStatisticsCardTopGenreBinding.inflate(LayoutInflater.from(context), this)

  private var topGenres = emptyList<Genre>()

  init {
    layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
    setCardBackgroundColor(context.colorFromAttr(R.attr.colorCardBackground))
    cardElevation = context.dimenToPx(R.dimen.elevationSmall).toFloat()
    strokeWidth = 0
    onClick {
      showGenres(10)
      isClickable = false
      binding.viewTopGenresSubValue.text = context.getString(R.string.textStatisticsTopGenreSubValue2)
    }
  }

  fun bind(genres: List<Genre>) {
    topGenres = genres.toList()
    showGenres(3)
  }

  private fun showGenres(limit: Int) {
    binding.viewTopGenresValue.text =
      topGenres
        .take(limit)
        .joinToString("\n") {
          context.getString(it.displayName)
        }
  }
}
