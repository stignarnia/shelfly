package xyz.stignarnia.ui_lists.manage.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import xyz.stignarnia.ui_base.utilities.extensions.addRipple
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_lists.databinding.ViewManageListsItemBinding
import xyz.stignarnia.ui_lists.manage.recycler.ManageListsItem

class ManageListsItemView : FrameLayout {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewManageListsItemBinding.inflate(LayoutInflater.from(context), this)

  var itemCheckListener: ((ManageListsItem, Boolean) -> Unit)? = null
  private var isCheckEnabled = false

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    with(binding) {
      addRipple()
      onClick(safe = false) {
        val isChecked = !manageListsItemCheckbox.isChecked
        itemCheckListener?.invoke(item, isChecked)
      }
    }
  }

  private lateinit var item: ManageListsItem

  fun bind(item: ManageListsItem) {
    this.item = item
    isCheckEnabled = false
    with(binding) {
      manageListsItemCheckbox.text = item.list.name
      manageListsItemCheckbox.isChecked = item.isChecked
      manageListsItemCheckbox.isEnabled = item.isEnabled
    }
    isCheckEnabled = true
  }
}
