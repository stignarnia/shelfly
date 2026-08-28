package xyz.stignarnia.uiPeople.details.recycler.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import xyz.stignarnia.common.Mode
import xyz.stignarnia.uiPeople.databinding.ViewPersonDetailsFiltersBinding
import xyz.stignarnia.uiPeople.details.filters.PersonDetailsFilters

class PersonDetailsFiltersView : FrameLayout {
  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewPersonDetailsFiltersBinding.inflate(LayoutInflater.from(context), this, true)

  init {
    with(binding) {
      viewPersonDetailsFiltersShowsChip.setOnCheckedChangeListener { _, _ -> onChipCheckChange() }
      viewPersonDetailsFiltersMoviesChip.setOnCheckedChangeListener { _, _ -> onChipCheckChange() }
      viewPersonDetailsFiltersCollectionChip.setOnCheckedChangeListener { _, _ -> onChipCheckChange() }
    }
  }

  var onChipsChangeListener: ((PersonDetailsFilters) -> Unit)? = null
  private var isListenerEnabled = true

  private fun onChipCheckChange() {
    if (!isListenerEnabled) {
      return
    }

    with(binding) {
      val ids =
        viewPersonDetailsFiltersChipGroup.checkedChipIds.mapNotNull {
          when (it) {
            viewPersonDetailsFiltersShowsChip.id -> Mode.SHOWS
            viewPersonDetailsFiltersMoviesChip.id -> Mode.MOVIES
            else -> null
          }
        }

      onChipsChangeListener?.invoke(
        PersonDetailsFilters(
          modes = ids,
          onlyCollection = viewPersonDetailsFiltersCollectionChip.isChecked,
        ),
      )
    }
  }

  override fun setEnabled(enabled: Boolean) {
    with(binding) {
      viewPersonDetailsFiltersShowsChip.isEnabled = enabled
      viewPersonDetailsFiltersMoviesChip.isEnabled = enabled
      viewPersonDetailsFiltersCollectionChip.isEnabled = enabled
    }
  }

  fun bind(filters: PersonDetailsFilters) {
    with(binding) {
      isListenerEnabled = false
      viewPersonDetailsFiltersShowsChip.isChecked = Mode.SHOWS in filters.modes
      viewPersonDetailsFiltersMoviesChip.isChecked = Mode.MOVIES in filters.modes
      viewPersonDetailsFiltersCollectionChip.isChecked = filters.onlyCollection
      isListenerEnabled = true
    }
  }
}
