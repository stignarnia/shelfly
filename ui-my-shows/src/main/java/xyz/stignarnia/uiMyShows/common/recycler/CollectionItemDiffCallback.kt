package xyz.stignarnia.uiMyShows.common.recycler

import androidx.recyclerview.widget.DiffUtil

class CollectionItemDiffCallback : DiffUtil.ItemCallback<CollectionListItem>() {
  override fun areItemsTheSame(
    oldItem: CollectionListItem,
    newItem: CollectionListItem,
  ): Boolean {
    val areMovies = oldItem is CollectionListItem.ShowItem && newItem is CollectionListItem.ShowItem
    val areFilters = oldItem is CollectionListItem.FiltersItem && newItem is CollectionListItem.FiltersItem

    return when {
      areMovies -> {
        areItemsTheSame(
          (oldItem),
          (newItem),
        )
      }

      areFilters -> {
        true
      }

      else -> {
        false
      }
    }
  }

  override fun areContentsTheSame(
    oldItem: CollectionListItem,
    newItem: CollectionListItem,
  ): Boolean =
    when (oldItem) {
      is CollectionListItem.ShowItem -> areContentsTheSame(oldItem, (newItem as CollectionListItem.ShowItem))
      is CollectionListItem.FiltersItem -> areContentsTheSame(oldItem, (newItem as CollectionListItem.FiltersItem))
    }

  private fun areItemsTheSame(
    oldItem: CollectionListItem.ShowItem,
    newItem: CollectionListItem.ShowItem,
  ): Boolean = oldItem.show.tmdbId == newItem.show.tmdbId

  private fun areContentsTheSame(
    oldItem: CollectionListItem.ShowItem,
    newItem: CollectionListItem.ShowItem,
  ): Boolean =
    oldItem.show.firstAired == newItem.show.firstAired &&
      oldItem.image == newItem.image &&
      oldItem.isLoading == newItem.isLoading &&
      oldItem.translation == newItem.translation &&
      oldItem.sortOrder == newItem.sortOrder &&
      oldItem.spoilers == newItem.spoilers &&
      oldItem.userRating == newItem.userRating

  private fun areContentsTheSame(
    oldItem: CollectionListItem.FiltersItem,
    newItem: CollectionListItem.FiltersItem,
  ): Boolean =
    oldItem.upcoming == newItem.upcoming &&
      oldItem.networks.toTypedArray().contentEquals(newItem.networks.toTypedArray()) &&
      oldItem.genres.toTypedArray().contentEquals(newItem.genres.toTypedArray()) &&
      oldItem.sortOrder == newItem.sortOrder &&
      oldItem.sortType == newItem.sortType &&
      oldItem.count == newItem.count
}
