package xyz.stignarnia.uiProgress.calendar.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiModel.CalendarMode
import xyz.stignarnia.uiProgress.R
import xyz.stignarnia.uiProgress.calendar.recycler.CalendarListItem
import xyz.stignarnia.uiProgress.databinding.ViewCalendarFiltersBinding

internal class CalendarFiltersView : FrameLayout {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewCalendarFiltersBinding.inflate(LayoutInflater.from(context), this)

  private lateinit var filters: CalendarListItem.Filters

  var onModeChipClick: ((CalendarMode) -> Unit)? = null
  var onPremieresChipClick: (() -> Unit)? = null

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    with(binding) {
      modeChip.onClick {
        onModeChipClick?.invoke(filters.mode)
      }
      premieresChip.onClick {
        onPremieresChipClick?.invoke()
      }
    }
  }

  fun bind(filters: CalendarListItem.Filters) {
    this.filters = filters
    with(binding) {
      premieresChip.isSelected = filters.premieres
      when (filters.mode) {
        CalendarMode.PRESENT_FUTURE -> {
          modeChip.text = context.getText(R.string.textWatchlistIncoming)
          modeChip.setChipIconResource(R.drawable.ic_calendar)
        }

        CalendarMode.RECENTS -> {
          modeChip.text = context.getText(R.string.textMovieStatusReleased)
          modeChip.setChipIconResource(R.drawable.ic_history)
        }
      }
    }
  }
}
