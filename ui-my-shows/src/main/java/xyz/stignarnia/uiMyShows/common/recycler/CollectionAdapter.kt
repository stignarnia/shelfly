package xyz.stignarnia.uiMyShows.common.recycler

import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import xyz.stignarnia.uiBase.BaseAdapter
import xyz.stignarnia.uiBase.BaseMovieAdapter
import xyz.stignarnia.uiBase.common.ListViewMode
import xyz.stignarnia.uiBase.common.ListViewMode.LIST_NORMAL
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiMyShows.common.recycler.CollectionListItem.FiltersItem
import xyz.stignarnia.uiMyShows.common.recycler.CollectionListItem.ShowItem
import xyz.stignarnia.uiMyShows.common.views.CollectionShowFiltersView
import xyz.stignarnia.uiMyShows.common.views.CollectionShowView

class CollectionAdapter(
  listChangeListener: () -> Unit,
  private val itemClickListener: (CollectionListItem) -> Unit,
  private val itemLongClickListener: (CollectionListItem) -> Unit,
  private val sortChipClickListener: (SortOrder, SortType) -> Unit,
  private val upcomingChipClickListener: () -> Unit,
  private val networksChipClickListener: () -> Unit,
  private val genresChipClickListener: () -> Unit,
  private val missingImageListener: (CollectionListItem, Boolean) -> Unit,
  private val missingTranslationListener: (CollectionListItem) -> Unit,
  private val upcomingChipVisible: Boolean = true,
) : BaseAdapter<CollectionListItem>(
    listChangeListener = listChangeListener,
  ) {
  companion object {
    private const val VIEW_TYPE_SHOW = 1
    private const val VIEW_TYPE_FILTERS = 2
  }

  override val asyncDiffer = AsyncListDiffer(this, CollectionItemDiffCallback())

  var listViewMode: ListViewMode = LIST_NORMAL
    set(value) {
      field = value
      notifyItemRangeChanged(0, asyncDiffer.currentList.size)
    }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ) = when (viewType) {
    VIEW_TYPE_SHOW -> {
      BaseMovieAdapter.BaseViewHolder(
        when (listViewMode) {
          LIST_NORMAL -> CollectionShowView(parent.context)
        }.apply {
          itemClickListener = this@CollectionAdapter.itemClickListener
          itemLongClickListener = this@CollectionAdapter.itemLongClickListener
          missingImageListener = this@CollectionAdapter.missingImageListener
          missingTranslationListener = this@CollectionAdapter.missingTranslationListener
        },
      )
    }

    VIEW_TYPE_FILTERS -> {
      BaseMovieAdapter.BaseViewHolder(
        CollectionShowFiltersView(parent.context).apply {
          onSortChipClicked = this@CollectionAdapter.sortChipClickListener
          onFilterUpcomingClicked = this@CollectionAdapter.upcomingChipClickListener
          onNetworksChipClick = this@CollectionAdapter.networksChipClickListener
          onGenresChipClick = this@CollectionAdapter.genresChipClickListener
          isUpcomingChipVisible = upcomingChipVisible
        },
      )
    }

    else -> {
      throw IllegalStateException()
    }
  }

  override fun onBindViewHolder(
    holder: RecyclerView.ViewHolder,
    position: Int,
  ) {
    when (val item = asyncDiffer.currentList[position]) {
      is FiltersItem -> {
        (holder.itemView as CollectionShowFiltersView).bind(item, listViewMode)
      }

      is ShowItem -> {
        when (listViewMode) {
          LIST_NORMAL -> (holder.itemView as CollectionShowView).bind(item)
        }
      }
    }
  }

  override fun getItemViewType(position: Int) =
    when (asyncDiffer.currentList[position]) {
      is ShowItem -> VIEW_TYPE_SHOW
      is FiltersItem -> VIEW_TYPE_FILTERS
      else -> throw IllegalStateException()
    }
}
