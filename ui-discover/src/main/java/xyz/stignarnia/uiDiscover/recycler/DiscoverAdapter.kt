package xyz.stignarnia.uiDiscover.recycler

import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import xyz.stignarnia.uiBase.BaseAdapter
import xyz.stignarnia.uiDiscover.views.ShowFanartView
import xyz.stignarnia.uiDiscover.views.ShowPosterView
import xyz.stignarnia.uiModel.ImageType.FANART
import xyz.stignarnia.uiModel.ImageType.FANART_WIDE
import xyz.stignarnia.uiModel.ImageType.POSTER

class DiscoverAdapter(
  private val itemClickListener: (DiscoverListItem) -> Unit,
  private val itemLongClickListener: (DiscoverListItem) -> Unit,
  private val missingImageListener: (DiscoverListItem, Boolean) -> Unit,
  listChangeListener: () -> Unit,
) : BaseAdapter<DiscoverListItem>(
    listChangeListener = listChangeListener,
  ) {
  override val asyncDiffer = AsyncListDiffer(this, DiscoverItemDiffCallback())

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ) = when (viewType) {
    POSTER.id -> {
      BaseViewHolder(
        ShowPosterView(parent.context).apply {
          itemClickListener = this@DiscoverAdapter.itemClickListener
          itemLongClickListener = this@DiscoverAdapter.itemLongClickListener
          missingImageListener = this@DiscoverAdapter.missingImageListener
        },
      )
    }

    FANART.id, FANART_WIDE.id -> {
      BaseViewHolder(
        ShowFanartView(parent.context).apply {
          itemClickListener = this@DiscoverAdapter.itemClickListener
          itemLongClickListener = this@DiscoverAdapter.itemLongClickListener
          missingImageListener = this@DiscoverAdapter.missingImageListener
        },
      )
    }

    else -> {
      throw IllegalStateException("Unknown view type.")
    }
  }

  override fun onBindViewHolder(
    holder: RecyclerView.ViewHolder,
    position: Int,
  ) {
    val item = asyncDiffer.currentList[position]
    when (holder.itemViewType) {
      POSTER.id -> {
        (holder.itemView as ShowPosterView).bind(item)
      }

      FANART.id, FANART_WIDE.id -> {
        (holder.itemView as ShowFanartView).bind(item)
      }
    }
  }

  override fun getItemViewType(position: Int) =
    asyncDiffer.currentList[position]
      .image.type.id
}
