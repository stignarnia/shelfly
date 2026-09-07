package xyz.stignarnia.uiStatisticsMovies.views.ratings.recycler

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import xyz.stignarnia.uiBase.BaseMovieAdapter
import xyz.stignarnia.uiStatisticsMovies.views.ratings.StatisticsMoviesRateItemView

class StatisticsMoviesRatingsAdapter(
  private val itemClickListener: (StatisticsMoviesRatingItem) -> Unit,
  private val missingImageListener: ((StatisticsMoviesRatingItem, Boolean) -> Unit)? = null,
) : BaseMovieAdapter<StatisticsMoviesRatingItem>() {
  override val asyncDiffer = AsyncListDiffer(this, StatisticsMoviesRatingsDiffCallback())

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ) = ViewHolderShow(
    StatisticsMoviesRateItemView(parent.context).apply {
      itemClickListener = this@StatisticsMoviesRatingsAdapter.itemClickListener
      missingImageListener = this@StatisticsMoviesRatingsAdapter.missingImageListener
    },
  )

  override fun onBindViewHolder(
    holder: RecyclerView.ViewHolder,
    position: Int,
  ) {
    val item = asyncDiffer.currentList[position]
    (holder.itemView as StatisticsMoviesRateItemView).bind(item)
  }

  class ViewHolderShow(
    itemView: View,
  ) : RecyclerView.ViewHolder(itemView)
}
