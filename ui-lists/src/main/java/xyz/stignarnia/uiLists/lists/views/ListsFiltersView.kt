package xyz.stignarnia.uiLists.lists.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiLists.R
import xyz.stignarnia.uiLists.databinding.ViewListsFiltersBinding
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType

class ListsFiltersView : FrameLayout {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewListsFiltersBinding.inflate(LayoutInflater.from(context), this)

  var onSortClickListener: ((SortOrder, SortType) -> Unit)? = null

  override fun setEnabled(enabled: Boolean) {
    binding.viewListsFiltersChipGroup.forEach {
      it.isEnabled = enabled
    }
  }

  fun setSorting(
    sortOrder: SortOrder,
    sortType: SortType,
  ) {
    with(binding) {
      viewListsFilterSortChip.text = context.getString(sortOrder.displayString)
      viewListsFilterSortChip.onClick {
        onSortClickListener?.invoke(sortOrder, sortType)
      }
      val sortIcon =
        when (sortType) {
          SortType.ASCENDING -> R.drawable.ic_arrow_alt_up
          SortType.DESCENDING -> R.drawable.ic_arrow_alt_down
        }
      viewListsFilterSortChip.closeIcon = ContextCompat.getDrawable(context, sortIcon)
    }
  }
}
