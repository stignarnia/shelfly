package xyz.stignarnia.uiShow.quicksetup.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import xyz.stignarnia.uiModel.Season
import xyz.stignarnia.uiShow.R
import xyz.stignarnia.uiShow.databinding.ViewQuickSetupHeaderBinding
import java.util.Locale

class QuickSetupHeaderView : FrameLayout {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewQuickSetupHeaderBinding.inflate(LayoutInflater.from(context), this)

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
  }

  fun bind(season: Season) {
    binding.viewQuickSetupHeaderTitle.text =
      String.format(Locale.ENGLISH, context.getString(R.string.textSeason), season.number)
  }
}
