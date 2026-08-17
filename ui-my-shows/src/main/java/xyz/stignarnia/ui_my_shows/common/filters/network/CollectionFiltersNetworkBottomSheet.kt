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
          add(chip.tag.toString())
        }
      }
    }

  private fun render(uiState: CollectionFiltersNetworkUiState) {
    with(uiState) {
      available?.let { renderNetworks(it, selected) }
    }
  }

  private fun renderNetworks(
    available: List<String>,
    selected: List<String>,
  ) {
    binding.networksChipGroup.removeAllViews()
    binding.clearButton.visibleIf(selected.isNotEmpty())

    available.forEach { network ->
      val chip = Chip(requireContext()).apply {
        tag = network
        text = network
        isCheckable = true
        isCheckedIconVisible = false
        shapeAppearanceModel = shapeAppearanceModel
          .toBuilder()
          .setAllCornerSizes(100f)
          .build()
        setEnsureMinTouchTargetSize(false)
        // Only the handful of broadcasters with bundled artwork gets an icon; the rest stand on their name, which is the one TMDB gave the show.
        networkIconProvider.getIcon(network)?.let { setChipIconResource(it) }
        chipBackgroundColor =
          ContextCompat.getColorStateList(requireContext(), R.color.selector_discover_chip_background)
        setChipStrokeColorResource(R.color.selector_discover_chip_stroke)
        setChipStrokeWidthResource(R.dimen.discoverFilterChipStroke)
        setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.selector_discover_chip_text))
        isChecked = network in selected
        setOnCheckedChangeListener { _, _ ->
          binding.clearButton.visibleIf(checkedNetworks().isNotEmpty())
        }
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
