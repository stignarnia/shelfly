package xyz.stignarnia.ui_streamings.recycler

import androidx.recyclerview.widget.DiffUtil
import xyz.stignarnia.ui_model.StreamingService

class StreamingItemDiffCallback : DiffUtil.ItemCallback<StreamingService>() {

  override fun areItemsTheSame(
    oldItem: StreamingService,
    newItem: StreamingService,
  ) = oldItem.name == newItem.name

  override fun areContentsTheSame(
    oldItem: StreamingService,
    newItem: StreamingService,
  ) = oldItem.name == newItem.name &&
    oldItem.imagePath == newItem.imagePath &&
    oldItem.link == newItem.link &&
    oldItem.options.size == newItem.options.size &&
    oldItem.options.containsAll(newItem.options)
}
