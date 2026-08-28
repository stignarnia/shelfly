package xyz.stignarnia.uiDiscoverMovies.filters.providers

import android.os.Bundle
import android.view.View
import androidx.core.view.forEach
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.uiBase.BaseBottomSheetFragment
import xyz.stignarnia.uiBase.utilities.StreamingProviderChips
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.screenHeight
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiDiscoverMovies.DiscoverMoviesFragment.Companion.REQUEST_DISCOVER_FILTERS
import xyz.stignarnia.uiDiscoverMovies.R
import xyz.stignarnia.uiDiscoverMovies.databinding.ViewDiscoverMoviesFiltersProvidersBinding
import xyz.stignarnia.uiDiscoverMovies.filters.providers.DiscoverMoviesFiltersProvidersUiEvent.ApplyFilters
import xyz.stignarnia.uiDiscoverMovies.filters.providers.DiscoverMoviesFiltersProvidersUiEvent.CloseFilters
import xyz.stignarnia.uiModel.StreamingProvider

@AndroidEntryPoint
internal class DiscoverMoviesFiltersProvidersBottomSheet :
  BaseBottomSheetFragment(R.layout.view_discover_movies_filters_providers) {
  private val viewModel by viewModels<DiscoverMoviesFiltersProvidersViewModel>()
  private val binding by viewBinding(ViewDiscoverMoviesFiltersProvidersBinding::bind)

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
      applyButton.onClick { viewModel.saveProviders(checkedProviders()) }
      clearButton.onClick {
        providersChipGroup.forEach { (it as Chip).isChecked = false }
        clearButton.visibleIf(false)
      }
    }
  }

  private fun checkedProviders(): List<StreamingProvider> =
    buildList {
      binding.providersChipGroup.forEach { chip ->
        if ((chip as Chip).isChecked) {
          (chip.tag as? StreamingProvider)?.let { add(it) }
        }
      }
    }

  private fun render(uiState: DiscoverMoviesFiltersProvidersUiState) {
    with(uiState) {
      binding.providersRegion.visibleIf(available != null)
      binding.providersError.visibleIf(isError)
      binding.providersRegion.text = getString(R.string.textDiscoverFilterProvidersRegion, regionName)
      available?.let { renderProviders(it, selected) }
    }
  }

  private fun renderProviders(
    available: List<StreamingProvider>,
    selected: List<StreamingProvider>,
  ) {
    // Rebuilding is only correct while the sheet has no unapplied edits of its own, which holds: the list arrives once and the state does not change again until the sheet closes.
    binding.providersChipGroup.removeAllViews()
    binding.clearButton.visibleIf(selected.isNotEmpty())

    val selectedIds = selected.map { it.id }
    available.forEach { provider ->
      val chip =
        StreamingProviderChips.create(
          fragment = this,
          provider = provider,
          isChecked = provider.id in selectedIds,
          onCheckedChange = { binding.clearButton.visibleIf(checkedProviders().isNotEmpty()) },
        )
      binding.providersChipGroup.addView(chip)
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
