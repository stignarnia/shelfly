package xyz.stignarnia.uiGallery.fanart.recycler

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import xyz.stignarnia.uiGallery.fanart.recycler.views.ArtGalleryFanartView
import xyz.stignarnia.uiGallery.fanart.recycler.views.ArtGalleryPosterView
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.ImageType
import xyz.stignarnia.uiModel.ImageType.FANART
import xyz.stignarnia.uiModel.ImageType.FANART_WIDE
import xyz.stignarnia.uiModel.ImageType.POSTER

class ArtGalleryAdapter(
  val onItemClickListener: (() -> Unit),
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
  companion object {
    private const val VIEW_TYPE_POSTER = 0
    private const val VIEW_TYPE_FANART = 1
  }

  private lateinit var type: ImageType
  private val asyncDiffer = AsyncListDiffer(this, ImageItemDiffCallback())

  fun setItems(
    items: List<Image>,
    type: ImageType,
  ) {
    this.type = type
    asyncDiffer.submitList(items)
  }

  fun getItem(index: Int) = asyncDiffer.currentList.getOrNull(index)

  override fun getItemViewType(position: Int) =
    when (type) {
      POSTER -> VIEW_TYPE_POSTER
      FANART, FANART_WIDE -> VIEW_TYPE_FANART
      else -> throw Error("Invalid type")
    }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ) = when (viewType) {
    VIEW_TYPE_POSTER -> {
      ViewHolderShow(
        ArtGalleryPosterView(parent.context).apply {
          onItemClickListener = this@ArtGalleryAdapter.onItemClickListener
        },
      )
    }

    VIEW_TYPE_FANART -> {
      ViewHolderShow(
        ArtGalleryFanartView(parent.context).apply {
          onItemClickListener = this@ArtGalleryAdapter.onItemClickListener
        },
      )
    }

    else -> {
      error("Invalid type")
    }
  }

  override fun onBindViewHolder(
    holder: RecyclerView.ViewHolder,
    position: Int,
  ) {
    when (val itemView = holder.itemView) {
      is ArtGalleryPosterView -> itemView.bind(asyncDiffer.currentList[position])
      is ArtGalleryFanartView -> itemView.bind(asyncDiffer.currentList[position])
    }
  }

  class ViewHolderShow(
    itemView: View,
  ) : RecyclerView.ViewHolder(itemView)

  override fun getItemCount() = asyncDiffer.currentList.size
}
