package xyz.stignarnia.ui_discover.filters.providers

import android.os.Bundle
import android.view.View
import androidx.core.view.forEach
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import xyz.stignarnia.ui_base.BaseBottomSheetFragment
import xyz.stignarnia.ui_base.utilities.StreamingProviderChips
import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_base.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.screenHeight
import xyz.stignarnia.ui_base.utilities.extensions.visibleIf
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_discover.DiscoverFragment.Companion.REQUEST_DISCOVER_FILTERS
import xyz.stignarnia.ui_discover.R
import xyz.stignarnia.ui_discover.databinding.ViewDiscoverFiltersProvidersBinding
import xyz.stignarnia.ui_discover.filters.providers.DiscoverFiltersProvidersUiEvent.ApplyFilters
import xyz.stignarnia.ui_discover.filters.providers.DiscoverFiltersProvidersUiEvent.CloseFilters
import xyz.stignarnia.ui_model.StreamingProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class DiscoverFiltersProvidersBottomSheet :
  BaseBottomSheetFragment(R.layout.view_discover_filters_providers) {

  private val viewModel by viewModels<DiscoverFiltersProvidersViewModel>()
  private val binding by viewBinding(ViewDiscoverFiltersProvidersBinding::bind)

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
      applyButton.onClick { saveProviders() }
      clearButton.onClick {
        providersChipGroup.forEach { (it as Chip).isChecked = false }
        clearButton.visibleIf(false)
      }
    }
  }

  private fun saveProviders() {
    val checked = checkedProviders()
    viewModel.saveProviders(checked)
  }

  private fun checkedProviders(): List<StreamingProvider> =
    buildList {
      binding.providersChipGroup.forEach { chip ->
        if ((chip as Chip).isChecked) {
          (chip.tag as? StreamingProvider)?.let { add(it) }
        }
      }
    }

  private fun render(uiState: DiscoverFiltersProvidersUiState) {
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
      val chip = StreamingProviderChips.create(
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
