package xyz.stignarnia.uiProgressMovies.calendar.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.core.view.updatePadding
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiProgressMovies.R
import xyz.stignarnia.uiProgressMovies.calendar.recycler.CalendarMovieListItem
import xyz.stignarnia.uiProgressMovies.databinding.ViewCalendarMoviesHeaderBinding

class CalendarMoviesHeaderView : LinearLayout {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewCalendarMoviesHeaderBinding.inflate(LayoutInflater.from(context), this)

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    orientation = HORIZONTAL
    updatePadding(
      top = context.dimenToPx(R.dimen.spaceBig),
      bottom = context.dimenToPx(R.dimen.spaceTiny),
      left = context.dimenToPx(R.dimen.itemMarginHorizontal),
      right = context.dimenToPx(R.dimen.itemMarginHorizontal),
    )
  }

  fun bind(
    item: CalendarMovieListItem.Header,
    position: Int,
  ) {
    with(binding) {
      calendarMoviesHeaderText.setText(item.textResId)
    }
    updatePadding(
      top = context.dimenToPx(if (position == 1) R.dimen.spaceMedium else R.dimen.spaceBig),
    )
  }
}
