package xyz.stignarnia.uiProgressMovies.progress.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiModel.SortType.ASCENDING
import xyz.stignarnia.uiModel.SortType.DESCENDING
import xyz.stignarnia.uiProgressMovies.R
import xyz.stignarnia.uiProgressMovies.databinding.ViewProgressMoviesFiltersBinding

class ProgressMoviesFiltersView : FrameLayout {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewProgressMoviesFiltersBinding.inflate(LayoutInflater.from(context), this)

  var onSortChipClicked: ((SortOrder, SortType) -> Unit)? = null

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
  }

  fun bind(
    sortOrder: SortOrder,
    sortType: SortType,
  ) {
    with(binding) {
      val sortIcon =
        when (sortType) {
          ASCENDING -> R.drawable.ic_arrow_alt_up
          DESCENDING -> R.drawable.ic_arrow_alt_down
        }
      progressFiltersSortingChip.closeIcon = ContextCompat.getDrawable(context, sortIcon)
      progressFiltersSortingChip.text = context.getText(sortOrder.displayString)
      progressFiltersSortingChip.onClick {
        onSortChipClicked?.invoke(sortOrder, sortType)
      }
    }
  }
}
