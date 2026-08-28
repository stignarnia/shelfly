package xyz.stignarnia.uiProgress.history.filters.views

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.constraintlayout.widget.ConstraintLayout
import xyz.stignarnia.uiBase.R
import xyz.stignarnia.uiBase.utilities.extensions.addRipple
import xyz.stignarnia.uiBase.utilities.extensions.colorFromAttr
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiModel.HistoryPeriod
import xyz.stignarnia.uiProgress.databinding.ViewHistoryPeriodFilterItemBinding

class HistoryPeriodItemView : ConstraintLayout {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewHistoryPeriodFilterItemBinding.inflate(LayoutInflater.from(context), this)

  var onItemClick: ((HistoryPeriod) -> Unit)? = null

  lateinit var item: HistoryPeriod

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    val paddingHorizontal = context.dimenToPx(R.dimen.spaceNormal)
    setPadding(paddingHorizontal, 0, paddingHorizontal, 0)
    addRipple()
    onClick(safe = false) { onItemClick?.invoke(item) }
  }

  fun bind(
    item: HistoryPeriod,
    isChecked: Boolean,
  ) {
    this.item = item
    with(binding) {
      badge.visibleIf(isChecked)
      with(nameText) {
        val color = if (isChecked) android.R.attr.textColorPrimary else android.R.attr.textColorSecondary
        val typeface = if (isChecked) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        setTextColor(context.colorFromAttr(color))
        setTypeface(typeface)
        text = context.getString(item.displayStringRes)
      }
    }
  }
}
