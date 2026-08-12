package xyz.stignarnia.ui_my_movies.mymovies.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import xyz.stignarnia.ui_base.common.ListViewMode
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.visibleIf
import xyz.stignarnia.ui_model.MyMoviesSection.ALL
import xyz.stignarnia.ui_model.MyMoviesSection.RECENTS
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortType
import xyz.stignarnia.ui_my_movies.R
import xyz.stignarnia.ui_my_movies.databinding.ViewMyMoviesHeaderBinding
import xyz.stignarnia.ui_my_movies.mymovies.recycler.MyMoviesItem
import java.util.Locale.ENGLISH

class MyMovieHeaderView : FrameLayout {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewMyMoviesHeaderBinding.inflate(LayoutInflater.from(context), this)

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    clipChildren = false
    clipToPadding = false
  }

  fun bind(
    item: MyMoviesItem.Header,
    viewMode: ListViewMode,
    sortClickListener: (SortOrder, SortType) -> Unit,
    genresClickListener: () -> Unit,
  ) {
    bindLabel(item)
    with(binding) {
      myMoviesFilterChipsScroll.visibleIf(item.section == ALL)
      myMoviesSortChip.visibleIf(item.sortOrder != null)
      myMoviesGenresChip.visibleIf(item.genres != null)

      item.sortOrder?.let { sortOrder ->
        myMoviesSortChip.text = context.getString(sortOrder.first.displayString)
        myMoviesSortChip.onClick {
          sortClickListener.invoke(sortOrder.first, sortOrder.second)
        }
        val sortIcon = when (sortOrder.second) {
          SortType.ASCENDING -> R.drawable.ic_arrow_alt_up
          SortType.DESCENDING -> R.drawable.ic_arrow_alt_down
        }
        myMoviesSortChip.closeIcon = ContextCompat.getDrawable(context, sortIcon)
      }

      item.genres?.let { genres ->
        myMoviesGenresChip.isSelected = genres.isNotEmpty()
        myMoviesGenresChip.onClick { genresClickListener.invoke() }
        myMoviesGenresChip.text = when {
          genres.isEmpty() -> context.getString(R.string.textGenres).filter { it.isLetter() }
          genres.size == 1 -> context.getString(genres.first().displayName)
          genres.size == 2 -> "${context.getString(genres[0].displayName)}, ${context.getString(genres[1].displayName)}"
          else -> "${context.getString(genres[0].displayName)}, " +
            "${context.getString(genres[1].displayName)} + ${genres.size - 2}"
        }
      }
    }
  }

  private fun bindLabel(item: MyMoviesItem.Header) {
    val headerLabel = context.getString(item.section.displayString)
    binding.myMoviesHeaderLabel.text = when (item.section) {
      RECENTS -> headerLabel
      else -> String.format(ENGLISH, "%s (%d)", headerLabel, item.itemCount)
    }
  }
}
