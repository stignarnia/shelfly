package xyz.stignarnia.uiMyShows.myshows.filters

import android.os.Bundle
import android.view.View
import androidx.core.view.forEach
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.uiBase.BaseBottomSheetFragment
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.screenHeight
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiModel.MyShowsSection
import xyz.stignarnia.uiModel.MyShowsSection.ALL
import xyz.stignarnia.uiModel.MyShowsSection.FINISHED
import xyz.stignarnia.uiModel.MyShowsSection.UPCOMING
import xyz.stignarnia.uiModel.MyShowsSection.WATCHING
import xyz.stignarnia.uiMyShows.R
import xyz.stignarnia.uiMyShows.databinding.ViewMyShowsTypeFiltersBinding
import xyz.stignarnia.uiMyShows.main.FollowedShowsFragment.Companion.REQUEST_MY_SHOWS_FILTERS
import xyz.stignarnia.uiMyShows.myshows.filters.MyShowsFiltersUiEvent.ApplyFilters
import xyz.stignarnia.uiMyShows.myshows.filters.MyShowsFiltersUiEvent.CloseFilters
import xyz.stignarnia.uiMyShows.myshows.filters.views.MyShowsFilterItemView

@AndroidEntryPoint
internal class MyShowsFiltersBottomSheet : BaseBottomSheetFragment(R.layout.view_my_shows_type_filters) {
  private val viewModel by viewModels<MyShowsFiltersViewModel>()
  private val binding by viewBinding(ViewMyShowsTypeFiltersBinding::bind)

  override fun getTheme(): Int = R.style.CustomBottomSheetDialog

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()

    launchAndRepeatStarted(
      { viewModel.uiState.collect { render(it) } },
      { viewModel.eventFlow.collect { handleEvent(it) } },
    )
  }

  private fun setupView() {
    val behavior: BottomSheetBehavior<*> = (dialog as BottomSheetDialog).behavior
    behavior.skipCollapsed = true
    behavior.maxHeight = (screenHeight() * 0.9).toInt()

    with(binding) {
      rootItemsLayout.removeAllViews()
      listOf(ALL, WATCHING, UPCOMING, FINISHED)
        .filter { it != MyShowsSection.RECENTS }
        .forEach { section ->
          val itemView =
            MyShowsFilterItemView(requireContext()).apply {
              onItemClickListener = { toggleItem(it) }
              bind(section, isChecked = false)
            }
          rootItemsLayout.addView(itemView)
        }
      applyButton.onClick { applyFilters() }
    }
  }

  private fun toggleItem(section: MyShowsSection) {
    with(binding) {
      rootItemsLayout.forEach {
        (it as MyShowsFilterItemView).bind(
          sectionType = it.sectionType,
          isChecked = it.sectionType == section,
        )
      }
    }
  }

  private fun applyFilters() {
    with(binding) {
      rootItemsLayout.forEach {
        if ((it as MyShowsFilterItemView).isChecked) {
          viewModel.applySectionType(it.sectionType)
        }
      }
    }
  }

  private fun render(uiState: MyShowsFiltersUiState) {
    with(uiState) {
      sectionType?.let { sectionType ->
        binding.rootItemsLayout.forEach {
          (it as MyShowsFilterItemView).bind(
            sectionType = it.sectionType,
            isChecked = it.sectionType == sectionType,
          )
        }
      }
    }
  }

  private fun handleEvent(event: Event<*>) {
    when (event) {
      is ApplyFilters -> {
        setFragmentResult(REQUEST_MY_SHOWS_FILTERS, Bundle.EMPTY)
        closeSheet()
      }

      is CloseFilters -> {
        closeSheet()
      }
    }
  }
}
