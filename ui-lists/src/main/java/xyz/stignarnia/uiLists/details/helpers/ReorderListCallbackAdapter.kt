package xyz.stignarnia.uiLists.details.helpers

import androidx.recyclerview.widget.RecyclerView

interface ReorderListCallbackAdapter {
  fun onItemSwiped(viewHolder: RecyclerView.ViewHolder)

  fun onItemMove(
    fromPosition: Int,
    toPosition: Int,
  ): Boolean

  fun onItemCleared()
}
