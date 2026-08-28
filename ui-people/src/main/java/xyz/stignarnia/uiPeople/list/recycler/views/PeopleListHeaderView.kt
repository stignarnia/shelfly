package xyz.stignarnia.uiPeople.list.recycler.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import xyz.stignarnia.uiModel.Person
import xyz.stignarnia.uiPeople.R
import xyz.stignarnia.uiPeople.databinding.ViewPeopleListHeaderBinding
import xyz.stignarnia.uiPeople.list.recycler.PeopleListItem

class PeopleListHeaderView : FrameLayout {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewPeopleListHeaderBinding.inflate(LayoutInflater.from(context), this)

  private lateinit var item: PeopleListItem.HeaderItem

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
  }

  fun bind(item: PeopleListItem.HeaderItem) {
    this.item = item
    with(binding) {
      viewPeopleListHeaderTitle.text =
        when (item.department) {
          Person.Department.ACTING -> context.getString(R.string.textActing)
          Person.Department.DIRECTING -> context.getString(R.string.textDirecting)
          Person.Department.WRITING -> context.getString(R.string.textWriting)
          Person.Department.SOUND -> context.getString(R.string.textMusic)
          else -> "-"
        }
      viewPeopleListHeaderSubtitle.text = item.mediaTitle
    }
  }
}
