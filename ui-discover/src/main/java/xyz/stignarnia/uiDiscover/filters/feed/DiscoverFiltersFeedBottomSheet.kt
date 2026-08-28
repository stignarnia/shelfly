package xyz.stignarnia.uiDiscover.filters.feed

import android.os.Bundle
import android.view.View
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
import xyz.stignarnia.uiDiscover.DiscoverFragment.Companion.REQUEST_DISCOVER_FILTERS
import xyz.stignarnia.uiDiscover.R
import xyz.stignarnia.uiDiscover.databinding.ViewDiscoverFiltersFeedBinding
import xyz.stignarnia.uiDiscover.filters.feed.DiscoverFiltersFeedUiEvent.ApplyFilters
import xyz.stignarnia.uiDiscover.filters.feed.DiscoverFiltersFeedUiEvent.CloseFilters
import xyz.stignarnia.uiModel.DiscoverFeed
import xyz.stignarnia.uiModel.DiscoverFeed.ANTICIPATED
import xyz.stignarnia.uiModel.DiscoverFeed.POPULAR
import xyz.stignarnia.uiModel.DiscoverFeed.RECENT
import xyz.stignarnia.uiModel.DiscoverFeed.TRENDING

@AndroidEntryPoint
internal class DiscoverFiltersFeedBottomSheet : BaseBottomSheetFragment(R.layout.view_discover_filters_feed) {
  private val viewModel by viewModels<DiscoverFiltersFeedViewModel>()
  private val binding by viewBinding(ViewDiscoverFiltersFeedBinding::bind)

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
      applyButton.onClick { saveFeedOrder() }
    }
  }

  private fun saveFeedOrder() {
    with(binding) {
      val feedOrder =
        when {
          feedChipHot.isChecked -> TRENDING
          feedChipTopRated.isChecked -> POPULAR
          feedChipRecent.isChecked -> ANTICIPATED
          feedChipNewest.isChecked -> RECENT
          else -> throw IllegalStateException()
        }
      viewModel.saveFeedOrder(feedOrder)
    }
  }

  private fun render(uiState: DiscoverFiltersFeedUiState) {
    with(uiState) {
      feedOrder?.let { renderFilters(it) }
    }
  }

  private fun renderFilters(feedOrder: DiscoverFeed) {
    with(binding) {
      feedChipHot.isChecked = feedOrder == TRENDING
      feedChipTopRated.isChecked = feedOrder == POPULAR
      feedChipRecent.isChecked = feedOrder == ANTICIPATED
      feedChipNewest.isChecked = feedOrder == RECENT
    }
  }

  private fun handleEvent(event: Event<*>) {
    when (event) {
      is ApplyFilters -> {
        setFragmentResult(REQUEST_DISCOVER_FILTERS, Bundle.EMPTY)
        closeSheet()
      }

      is CloseFilters -> {
        closeSheet()
      }
    }
  }
}
