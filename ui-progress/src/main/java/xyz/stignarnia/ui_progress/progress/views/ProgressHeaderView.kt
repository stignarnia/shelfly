package xyz.stignarnia.ui_progress.progress.views

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_progress.databinding.ViewProgressHeaderBinding
import xyz.stignarnia.ui_progress.progress.recycler.ProgressListItem
import java.util.Locale

class ProgressHeaderView : LinearLayout {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewProgressHeaderBinding.inflate(LayoutInflater.from(context), this)

  var headerClickListener: ((ProgressListItem.Header) -> Unit)? = null

  private lateinit var item: ProgressListItem.Header
  private val isRtl by lazy { TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL }

  init {
    layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
    orientation = HORIZONTAL
    onClick { headerClickListener?.invoke(item) }
  }

  fun bind(item: ProgressListItem.Header) {
    this.item = item

    val rotation = if (isRtl) -90F else 90F
    binding.progressHeaderArrow.rotation = if (item.isCollapsed) 0F else rotation
    binding.progressHeaderText.setText(item.textResId)
  }
}
