package xyz.stignarnia.ui_my_shows.common.filters.network

import android.annotation.SuppressLint
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
import xyz.stignarnia.ui_base.utilities.extensions.requireSerializable
import xyz.stignarnia.ui_base.utilities.extensions.screenHeight
import xyz.stignarnia.ui_base.utilities.extensions.visibleIf
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_model.StreamingProvider
import xyz.stignarnia.ui_my_shows.R
import xyz.stignarnia.ui_my_shows.common.filters.CollectionFiltersOrigin
import xyz.stignarnia.ui_my_shows.common.filters.CollectionFiltersUiEvent.ApplyFilters
import xyz.stignarnia.ui_my_shows.common.filters.CollectionFiltersUiEvent.CloseFilters
import xyz.stignarnia.ui_my_shows.databinding.ViewFiltersNetworksBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class CollectionFiltersNetworkBottomSheet : BaseBottomSheetFragment(R.layout.view_filters_networks) {

  companion object {
    private const val ARG_ORIGIN = "ARG_ORIGIN"
    const val REQUEST_COLLECTION_FILTERS_NETWORK = "REQUEST_COLLECTION_FILTERS_NETWORK"

    fun createBundle(origin: CollectionFiltersOrigin): Bundle = Bundle().apply { putSerializable(ARG_ORIGIN, origin) }
  }

  private val viewModel by viewModels<CollectionFiltersNetworkViewModel>()
  private val binding by viewBinding(ViewFiltersNetworksBinding::bind)

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
      doAfterLaunch = {
        val origin = requireSerializable<CollectionFiltersOrigin>(ARG_ORIGIN)
        viewModel.loadData(origin)
      },
    )
  }

  @SuppressLint("SetTextI18n")
  private fun setupView() {
    val behavior: BottomSheetBehavior<*> = (dialog as BottomSheetDialog).behavior
    behavior.skipCollapsed = true
    behavior.maxHeight = (screenHeight() * 0.9).toInt()

    with(binding) {
      applyButton.onClick { viewModel.saveNetworks(checkedNetworks()) }
      clearButton.onClick {
        networksChipGroup.forEach { (it as Chip).isChecked = false }
        clearButton.visibleIf(false)
      }
    }
  }

  private fun checkedNetworks(): List<String> =
    buildList {
      binding.networksChipGroup.forEach { chip ->
        if ((chip as Chip).isChecked) {
          val provider = chip.tag as? StreamingProvider
          add(provider?.name ?: chip.text.toString())
        }
      }
    }

  private fun render(uiState: CollectionFiltersNetworkUiState) {
    with(uiState) {
      available?.let { renderNetworks(it, selected) }
    }
  }

  private fun renderNetworks(
    available: List<StreamingProvider>,
    selected: List<String>,
  ) {
    binding.networksChipGroup.removeAllViews()
    binding.clearButton.visibleIf(selected.isNotEmpty())

    available.forEach { provider ->
      val chip = StreamingProviderChips.create(
        fragment = this,
        provider = provider,
        isChecked = provider.name in selected,
        onCheckedChange = { binding.clearButton.visibleIf(checkedNetworks().isNotEmpty()) },
      )
      binding.networksChipGroup.addView(chip)
    }
  }

  private fun handleEvent(event: Event<*>) {
    when (event) {
      is ApplyFilters -> {
        setFragmentResult(REQUEST_COLLECTION_FILTERS_NETWORK, Bundle.EMPTY)
        closeSheet()
      }
      is CloseFilters -> {
        closeSheet()
      }
    }
  }
}
