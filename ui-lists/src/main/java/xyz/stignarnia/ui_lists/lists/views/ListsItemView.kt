package xyz.stignarnia.ui_lists.lists.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import xyz.stignarnia.ui_base.utilities.extensions.capitalizeWords
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.visibleIf
import xyz.stignarnia.ui_lists.databinding.ViewListsItemBinding
import xyz.stignarnia.ui_lists.lists.helpers.ListsItemImage
import xyz.stignarnia.ui_lists.lists.recycler.ListsItem
import xyz.stignarnia.ui_model.SortOrder.DATE_UPDATED
import xyz.stignarnia.ui_model.SortOrder.NAME
import xyz.stignarnia.ui_model.SortOrder.NEWEST

class ListsItemView : FrameLayout {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewListsItemBinding.inflate(LayoutInflater.from(context), this)

  var itemClickListener: ((ListsItem) -> Unit)? = null
  var missingImageListener: ((ListsItem, ListsItemImage, Boolean) -> Unit)? = null

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    with(binding) {
      listsItemRoot.onClick { itemClickListener?.invoke(item) }
      listsItemImages.missingImageListener = { itemImage, force ->
        missingImageListener?.invoke(item, itemImage, force)
      }
    }
  }

  private lateinit var item: ListsItem

  fun bind(item: ListsItem) {
    this.item = item
    with(binding) {
      listsItemTitle.text = item.list.name
      with(listsItemDescription) {
        text = item.list.description
        visibleIf(!item.list.description.isNullOrBlank())
      }

      val sortOrder = item.sortOrder.first
      listsItemHeader.visibleIf(sortOrder != NAME)
      listsItemHeader.text = when (sortOrder) {
        NAME -> ""
        NEWEST -> item.dateFormat?.format(item.list.createdAt)?.capitalizeWords()
        DATE_UPDATED -> item.dateFormat?.format(item.list.updatedAt)?.capitalizeWords()
        else -> throw IllegalStateException()
      }

      listsItemImages.bind(item.images)
    }
  }
}
