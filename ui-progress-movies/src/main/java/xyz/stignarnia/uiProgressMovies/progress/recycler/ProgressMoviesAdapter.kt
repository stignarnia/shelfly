package xyz.stignarnia.uiProgressMovies.progress.recycler

import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import xyz.stignarnia.uiBase.BaseMovieAdapter
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiProgressMovies.progress.views.ProgressMoviesFiltersView
import xyz.stignarnia.uiProgressMovies.progress.views.ProgressMoviesItemView

class ProgressMoviesAdapter(
  private val itemClickListener: (ProgressMovieListItem.MovieItem) -> Unit,
  private val itemLongClickListener: (ProgressMovieListItem.MovieItem) -> Unit,
  private val sortChipClickListener: (SortOrder, SortType) -> Unit,
  private val missingImageListener: (ProgressMovieListItem.MovieItem, Boolean) -> Unit,
  private val missingTranslationListener: (ProgressMovieListItem.MovieItem) -> Unit,
  private val checkClickListener: (ProgressMovieListItem.MovieItem) -> Unit,
  listChangeListener: () -> Unit,
) : BaseMovieAdapter<ProgressMovieListItem>(
    listChangeListener = listChangeListener,
  ) {
  companion object {
    private const val VIEW_TYPE_MOVIE = 1
    private const val VIEW_TYPE_FILTERS = 2
  }

  override val asyncDiffer = AsyncListDiffer(this, ProgressMovieItemDiffCallback())

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ): BaseViewHolder =
    when (viewType) {
      VIEW_TYPE_MOVIE -> {
        BaseViewHolder(
          ProgressMoviesItemView(parent.context).apply {
            itemClickListener = this@ProgressMoviesAdapter.itemClickListener
            itemLongClickListener = this@ProgressMoviesAdapter.itemLongClickListener
            checkClickListener = this@ProgressMoviesAdapter.checkClickListener
            missingImageListener = this@ProgressMoviesAdapter.missingImageListener
            missingTranslationListener = this@ProgressMoviesAdapter.missingTranslationListener
          },
        )
      }

      VIEW_TYPE_FILTERS -> {
        BaseViewHolder(
          ProgressMoviesFiltersView(parent.context).apply {
            onSortChipClicked = this@ProgressMoviesAdapter.sortChipClickListener
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
      is ProgressMovieListItem.FiltersItem -> {
        (holder.itemView as ProgressMoviesFiltersView).bind(item.sortOrder, item.sortType)
      }

      is ProgressMovieListItem.MovieItem -> {
        (holder.itemView as ProgressMoviesItemView).bind(item)
      }

      else -> {
        throw IllegalStateException()
      }
    }
  }

  override fun getItemViewType(position: Int): Int =
    when (asyncDiffer.currentList[position]) {
      is ProgressMovieListItem.MovieItem -> VIEW_TYPE_MOVIE
      is ProgressMovieListItem.FiltersItem -> VIEW_TYPE_FILTERS
      else -> throw IllegalStateException()
    }
}
