package xyz.stignarnia.ui_my_shows.common.filters.network

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.forEach
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import xyz.stignarnia.ui_base.BaseBottomSheetFragment
import xyz.stignarnia.ui_base.utilities.NetworkIconProvider
import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_base.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.requireSerializable
import xyz.stignarnia.ui_base.utilities.extensions.screenHeight
import xyz.stignarnia.ui_base.utilities.extensions.visibleIf
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_model.Network
import xyz.stignarnia.ui_my_shows.R
import xyz.stignarnia.ui_my_shows.common.filters.CollectionFiltersOrigin
import xyz.stignarnia.ui_my_shows.common.filters.CollectionFiltersUiEvent.ApplyFilters
import xyz.stignarnia.ui_my_shows.common.filters.CollectionFiltersUiEvent.CloseFilters
import xyz.stignarnia.ui_my_shows.databinding.ViewFiltersNetworksBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class CollectionFiltersNetworkBottomSheet : BaseBottomSheetFragment(R.layout.view_filters_networks) {

  companion object {
    private const val ARG_ORIGIN = "ARG_ORIGIN"
    const val REQUEST_COLLECTION_FILTERS_NETWORK = "REQUEST_COLLECTION_FILTERS_NETWORK"

    fun createBundle(origin: CollectionFiltersOrigin): Bundle = bundleOf(ARG_ORIGIN to origin)
  }

  private val viewModel by viewModels<CollectionFiltersNetworkViewModel>()
  private val binding by viewBinding(ViewFiltersNetworksBinding::bind)

  @Inject
  lateinit var networkIconProvider: NetworkIconProvider

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
      applyButton.onClick { saveNetworks() }
      clearButton.onClick { renderNetworks(emptyList()) }
    }
  }

  private fun saveNetworks() {
    with(binding) {
      val networks = mutableListOf<Network>().apply {
        networksChipGroup.forEach { chip ->
          if ((chip as Chip).isChecked) {
            add(Network.valueOf(chip.tag.toString()))
          }
        }
      }
      viewModel.saveNetworks(networks)
    }
  }

  private fun render(uiState: CollectionFiltersNetworkUiState) {
    with(uiState) {
      networks?.let { renderNetworks(it) }
    }
  }

  private fun renderNetworks(networks: List<Network>) {
    binding.networksChipGroup.removeAllViews()
    binding.clearButton.visibleIf(networks.isNotEmpty())

    val networksNames = networks.map { it.name }
    Network
      .values()
      .sortedBy { it.name }
      .forEach { network ->
        val icon = networkIconProvider.getIcon(network)
        val chip = Chip(requireContext()).apply {
          tag = network.name
          text = network.channels.first()
          isCheckable = true
          isCheckedIconVisible = false
          shapeAppearanceModel = shapeAppearanceModel
            .toBuilder()
            .setAllCornerSizes(100f)
            .build()
          setEnsureMinTouchTargetSize(false)
          setChipIconResource(icon)
          chipBackgroundColor =
            ContextCompat.getColorStateList(requireContext(), R.color.selector_discover_chip_background)
          setChipStrokeColorResource(R.color.selector_discover_chip_stroke)
          setChipStrokeWidthResource(R.dimen.discoverFilterChipStroke)
          setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.selector_discover_chip_text))
          isChecked = network.name in networksNames
        }
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
