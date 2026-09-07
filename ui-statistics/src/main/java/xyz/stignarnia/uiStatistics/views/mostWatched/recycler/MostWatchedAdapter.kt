package xyz.stignarnia.uiStatistics.views.mostWatched.recycler

import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import xyz.stignarnia.uiBase.BaseAdapter
import xyz.stignarnia.uiStatistics.views.mostWatched.StatisticsMostWatchedItem
import xyz.stignarnia.uiStatistics.views.mostWatched.StatisticsMostWatchedItemView

class MostWatchedAdapter(
  private val itemClickListener: (StatisticsMostWatchedItem) -> Unit,
  private val missingImageListener: ((StatisticsMostWatchedItem, Boolean) -> Unit)? = null,
) : BaseAdapter<StatisticsMostWatchedItem>() {
  override val asyncDiffer = AsyncListDiffer(this, MostWatchedItemDiffCallback())

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ) = BaseViewHolder(
    StatisticsMostWatchedItemView(parent.context).apply {
      itemClickListener = this@MostWatchedAdapter.itemClickListener
      missingImageListener = this@MostWatchedAdapter.missingImageListener
    },
  )

  override fun onBindViewHolder(
    holder: RecyclerView.ViewHolder,
    position: Int,
  ) {
    val item = asyncDiffer.currentList[position]
    (holder.itemView as StatisticsMostWatchedItemView).bind(item)
  }
}
