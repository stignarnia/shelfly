package xyz.stignarnia.uiProgress.history.filters

import android.os.Bundle
import android.view.View
import androidx.core.view.children
import androidx.fragment.app.setFragmentResult
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.uiBase.BaseBottomSheetFragment
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.requireSerializable
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiModel.HistoryPeriod
import xyz.stignarnia.uiModel.HistoryPeriod.ALL_TIME
import xyz.stignarnia.uiProgress.R
import xyz.stignarnia.uiProgress.databinding.FragmentHistoryPeriodFilterBinding
import xyz.stignarnia.uiProgress.history.filters.views.HistoryPeriodItemView

@AndroidEntryPoint
class HistoryPeriodFilterBottomSheet : BaseBottomSheetFragment(R.layout.fragment_history_period_filter) {
  companion object {
    const val REQUEST_KEY = "REQUEST_KEY_HISTORY_DATES_FILTER"
    const val ARG_SELECTED_FILTER = "ARG_SELECTED_ITEM"

    fun createBundle(selectedItem: HistoryPeriod): Bundle =
      Bundle().apply { putSerializable(ARG_SELECTED_FILTER, selectedItem) }
  }

  private val binding by viewBinding(FragmentHistoryPeriodFilterBinding::bind)

  private lateinit var initialPeriod: HistoryPeriod
  private lateinit var selectedPeriod: HistoryPeriod

  override fun getTheme(): Int = R.style.CustomBottomSheetDialog

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    initialPeriod = requireSerializable(ARG_SELECTED_FILTER)
    selectedPeriod = initialPeriod
    setupView()
  }

  private fun setupView() {
    with(binding) {
      itemsLayout.removeAllViews()
      HistoryPeriod.entries
        .filterNot { it == ALL_TIME }
        .forEach { item ->
          val view =
            HistoryPeriodItemView(requireContext()).apply {
              bind(item, isChecked = item == initialPeriod)
              onItemClick = {
                selectedPeriod = it
                itemsLayout.children.forEach { view ->
                  (view as HistoryPeriodItemView).bind(view.item, view.item == selectedPeriod)
                }
              }
            }
          itemsLayout.addView(view)
        }
      applyButton.onClick { applyFilter() }
    }
  }

  private fun applyFilter() {
    if (selectedPeriod == initialPeriod) {
      closeSheet()
      return
    }
    val result = Bundle().apply { putSerializable(ARG_SELECTED_FILTER, selectedPeriod) }
    setFragmentResult(REQUEST_KEY, result)
    closeSheet()
  }
}
