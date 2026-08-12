package xyz.stignarnia.ui_discover_movies.filters.feed

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import xyz.stignarnia.ui_base.BaseBottomSheetFragment
import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_base.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.screenHeight
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_discover_movies.DiscoverMoviesFragment.Companion.REQUEST_DISCOVER_FILTERS
import xyz.stignarnia.ui_discover_movies.R
import xyz.stignarnia.ui_discover_movies.databinding.ViewDiscoverMoviesFiltersFeedBinding
import xyz.stignarnia.ui_discover_movies.filters.feed.DiscoverMoviesFiltersFeedUiEvent.ApplyFilters
import xyz.stignarnia.ui_discover_movies.filters.feed.DiscoverMoviesFiltersFeedUiEvent.CloseFilters
import xyz.stignarnia.ui_model.DiscoverFeed
import xyz.stignarnia.ui_model.DiscoverFeed.ANTICIPATED
import xyz.stignarnia.ui_model.DiscoverFeed.POPULAR
import xyz.stignarnia.ui_model.DiscoverFeed.RECENT
import xyz.stignarnia.ui_model.DiscoverFeed.TRENDING
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class DiscoverMoviesFiltersFeedBottomSheet :
  BaseBottomSheetFragment(
    R.layout.view_discover_movies_filters_feed,
  ) {

  private val viewModel by viewModels<DiscoverMoviesFiltersFeedViewModel>()
  private val binding by viewBinding(ViewDiscoverMoviesFiltersFeedBinding::bind)

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

  @SuppressLint("SetTextI18n")
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
      val feedOrder = when {
        feedChipTrending.isChecked -> TRENDING
        feedChipPopular.isChecked -> POPULAR
        feedChipAnticipated.isChecked -> ANTICIPATED
        feedChipNewest.isChecked -> RECENT
        else -> throw IllegalStateException()
      }
      viewModel.saveFeedOrder(feedOrder)
    }
  }

  private fun render(uiState: DiscoverMoviesFiltersFeedUiState) {
    with(uiState) {
      feedOrder?.let { renderFilters(it) }
    }
  }

  private fun renderFilters(feedOrder: DiscoverFeed) {
    with(binding) {
      feedChipTrending.isChecked = feedOrder == TRENDING
      feedChipPopular.isChecked = feedOrder == POPULAR
      feedChipAnticipated.isChecked = feedOrder == ANTICIPATED
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
