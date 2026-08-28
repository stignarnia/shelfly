package xyz.stignarnia.uiBase.common.sheets.sortOrder.views

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.constraintlayout.widget.ConstraintLayout
import xyz.stignarnia.uiBase.R
import xyz.stignarnia.uiBase.databinding.ViewSortOrderItemBinding
import xyz.stignarnia.uiBase.utilities.extensions.addRipple
import xyz.stignarnia.uiBase.utilities.extensions.colorFromAttr
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType

class SortOrderItemView : ConstraintLayout {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewSortOrderItemBinding.inflate(LayoutInflater.from(context), this)

  var onItemClickListener: ((SortOrder, SortType) -> Unit)? = null

  lateinit var sortOrder: SortOrder
  lateinit var sortType: SortType
  var isChecked: Boolean = false

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    val paddingHorizontal = context.dimenToPx(R.dimen.spaceNormal)
    setPadding(paddingHorizontal, 0, paddingHorizontal, 0)
    addRipple()
    onClick(safe = false) { onItemClickListener?.invoke(sortOrder, sortType) }
  }

  fun bind(
    sortOrder: SortOrder,
    sortType: SortType,
    isChecked: Boolean,
    animate: Boolean = false,
  ) {
    this.sortOrder = sortOrder
    this.sortType = sortType
    this.isChecked = isChecked

    with(binding) {
      viewSortOrderItemBadge.visibleIf(isChecked)

      with(viewSortOrderItemTitle) {
        val color = if (isChecked) android.R.attr.textColorPrimary else android.R.attr.textColorSecondary
        val typeface = if (isChecked) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        setTextColor(context.colorFromAttr(color))
        setTypeface(typeface)
        text = context.getString(sortOrder.displayString)
      }

      with(viewSortOrderItemAscDesc) {
        visibleIf(isChecked)
        val rotation = if (sortType == SortType.ASCENDING) -90F else 90F
        val duration = if (animate) 200L else 0
        animate().rotation(rotation).setDuration(duration).start()
      }
    }
  }
}
