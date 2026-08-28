package xyz.stignarnia.uiGallery.fanart.recycler

import androidx.recyclerview.widget.DiffUtil
import xyz.stignarnia.uiModel.Image

class ImageItemDiffCallback : DiffUtil.ItemCallback<Image>() {
  override fun areItemsTheSame(
    oldItem: Image,
    newItem: Image,
  ) = oldItem.id == newItem.id

  override fun areContentsTheSame(
    oldItem: Image,
    newItem: Image,
  ) = oldItem == newItem
}
