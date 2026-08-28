package xyz.stignarnia.uiPeople.details.recycler.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import xyz.stignarnia.uiPeople.R
import xyz.stignarnia.uiPeople.databinding.ViewPersonDetailsHeaderBinding
import xyz.stignarnia.uiPeople.details.recycler.PersonDetailsItem

class PersonDetailsHeaderView : FrameLayout {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewPersonDetailsHeaderBinding.inflate(LayoutInflater.from(context), this)

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
  }

  fun bind(item: PersonDetailsItem.CreditsHeader) {
    binding.viewPersonDetailsHeader.text =
      if (item.year != null) {
        item.year.toString()
      } else {
        context.getString(R.string.textMovieStatusInProduction)
      }
  }
}
