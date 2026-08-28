package xyz.stignarnia.uiLists.details.helpers

import androidx.recyclerview.widget.RecyclerView

interface ListItemDragListener {
  fun onListItemDragStarted(viewHolder: RecyclerView.ViewHolder)
}
