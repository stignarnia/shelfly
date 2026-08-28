package xyz.stignarnia.uiDiscoverMovies.recycler

import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import xyz.stignarnia.uiBase.BaseMovieAdapter
import xyz.stignarnia.uiDiscoverMovies.views.MovieFanartView
import xyz.stignarnia.uiDiscoverMovies.views.MoviePosterView
import xyz.stignarnia.uiModel.ImageType.FANART
import xyz.stignarnia.uiModel.ImageType.FANART_WIDE
import xyz.stignarnia.uiModel.ImageType.POSTER

class DiscoverMoviesAdapter(
  private val itemClickListener: (DiscoverMovieListItem) -> Unit,
  private val itemLongClickListener: (DiscoverMovieListItem) -> Unit,
  private val missingImageListener: (DiscoverMovieListItem, Boolean) -> Unit,
  listChangeListener: () -> Unit,
) : BaseMovieAdapter<DiscoverMovieListItem>(
    listChangeListener = listChangeListener,
  ) {
  init {
    stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
  }

  override val asyncDiffer = AsyncListDiffer(this, DiscoverMovieItemDiffCallback())

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ) = when (viewType) {
    POSTER.id -> {
      BaseViewHolder(
        MoviePosterView(parent.context).apply {
          itemClickListener = this@DiscoverMoviesAdapter.itemClickListener
          itemLongClickListener = this@DiscoverMoviesAdapter.itemLongClickListener
          missingImageListener = this@DiscoverMoviesAdapter.missingImageListener
        },
      )
    }

    FANART.id, FANART_WIDE.id -> {
      BaseViewHolder(
        MovieFanartView(parent.context).apply {
          itemClickListener = this@DiscoverMoviesAdapter.itemClickListener
          itemLongClickListener = this@DiscoverMoviesAdapter.itemLongClickListener
          missingImageListener = this@DiscoverMoviesAdapter.missingImageListener
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
        (holder.itemView as MoviePosterView).bind(item)
      }

      FANART.id, FANART_WIDE.id -> {
        (holder.itemView as MovieFanartView).bind(item)
      }
    }
  }

  override fun getItemViewType(position: Int) =
    asyncDiffer.currentList[position]
      .image.type.id
}
