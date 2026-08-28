package xyz.stignarnia.uiMyShows.myshows.recycler

import androidx.recyclerview.widget.DiffUtil
import xyz.stignarnia.uiMyShows.myshows.recycler.MyShowsItem.Type.ALL_SHOWS_HEADER
import xyz.stignarnia.uiMyShows.myshows.recycler.MyShowsItem.Type.RECENT_SHOWS

class MyShowsItemDiffCallback : DiffUtil.ItemCallback<MyShowsItem>() {
  override fun areItemsTheSame(
    oldItem: MyShowsItem,
    newItem: MyShowsItem,
  ) = when (oldItem.type) {
    RECENT_SHOWS -> true
    else -> oldItem.type == newItem.type && oldItem.show.ids.tmdb == newItem.show.ids.tmdb
  }

  override fun areContentsTheSame(
    oldItem: MyShowsItem,
    newItem: MyShowsItem,
  ) = when (oldItem.type) {
    ALL_SHOWS_HEADER -> {
      oldItem.header == newItem.header
    }

    RECENT_SHOWS -> {
      oldItem.recentsSection == newItem.recentsSection
    }

    else -> {
      oldItem.image == newItem.image &&
        oldItem.isLoading == newItem.isLoading &&
        oldItem.translation == newItem.translation &&
        oldItem.userRating == newItem.userRating &&
        oldItem.spoilers == newItem.spoilers &&
        oldItem.sortOrder == newItem.sortOrder
    }
  }
}
