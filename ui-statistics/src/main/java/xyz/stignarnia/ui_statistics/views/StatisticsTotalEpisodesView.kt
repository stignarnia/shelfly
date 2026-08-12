package xyz.stignarnia.ui_statistics.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.google.android.material.card.MaterialCardView
import xyz.stignarnia.ui_base.utilities.extensions.colorFromAttr
import xyz.stignarnia.ui_base.utilities.extensions.dimenToPx
import xyz.stignarnia.ui_statistics.R
import xyz.stignarnia.ui_statistics.databinding.ViewStatisticsCardTotalEpisodesBinding
import java.text.NumberFormat
import java.util.Locale

@SuppressLint("SetTextI18n")
class StatisticsTotalEpisodesView : MaterialCardView {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewStatisticsCardTotalEpisodesBinding.inflate(LayoutInflater.from(context), this)

  init {
    layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
    setCardBackgroundColor(context.colorFromAttr(R.attr.colorCardBackground))
    cardElevation = context.dimenToPx(R.dimen.elevationSmall).toFloat()
    strokeWidth = 0
  }

  fun bind(
    episodesCount: Int,
    episodesShowsCount: Int,
  ) {
    val formatter = NumberFormat.getNumberInstance(Locale.ENGLISH)
    with(binding) {
      viewTotalEpisodesValue.text = context.getString(
        R.string.textStatisticsTotalEpisodesCount,
        formatter.format(episodesCount),
      )
      viewTotalEpisodesSubValue.text = context.getString(
        R.string.textStatisticsTotalEpisodesShowsCount,
        formatter.format(episodesShowsCount),
      )
    }
  }
}
