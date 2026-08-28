package xyz.stignarnia.uiStatistics.views.ratings

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.card.MaterialCardView
import xyz.stignarnia.uiBase.common.ListItem
import xyz.stignarnia.uiBase.utilities.extensions.addDivider
import xyz.stignarnia.uiBase.utilities.extensions.colorFromAttr
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiStatistics.R
import xyz.stignarnia.uiStatistics.databinding.ViewStatisticsCardRatingsBinding
import xyz.stignarnia.uiStatistics.views.ratings.recycler.StatisticsRatingItem
import xyz.stignarnia.uiStatistics.views.ratings.recycler.StatisticsRatingsAdapter

class StatisticsRatingsView : MaterialCardView {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewStatisticsCardRatingsBinding.inflate(LayoutInflater.from(context), this)

  private lateinit var adapter: StatisticsRatingsAdapter
  private val layoutManager by lazy { LinearLayoutManager(context, HORIZONTAL, false) }

  var onShowClickListener: ((ListItem) -> Unit)? = null

  init {
    layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
    clipToPadding = false
    clipChildren = false
    cardElevation = context.dimenToPx(R.dimen.elevationSmall).toFloat()
    strokeWidth = 0
    setCardBackgroundColor(context.colorFromAttr(R.attr.colorCardBackground))
    setupRecycler()
  }

  private fun setupRecycler() {
    adapter =
      StatisticsRatingsAdapter(
        itemClickListener = { onShowClickListener?.invoke(it) },
      )
    binding.viewRatingsRecycler.apply {
      setHasFixedSize(true)
      adapter = this@StatisticsRatingsView.adapter
      layoutManager = this@StatisticsRatingsView.layoutManager
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
      addDivider(R.drawable.divider_statistics_ratings, HORIZONTAL)
    }
  }

  fun bind(items: List<StatisticsRatingItem>) {
    adapter.setItems(items)
  }
}
