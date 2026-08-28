package xyz.stignarnia.uiDiscover.filters.genres

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.uiBase.BaseBottomSheetFragment
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.screenHeight
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiDiscover.DiscoverFragment.Companion.REQUEST_DISCOVER_FILTERS
import xyz.stignarnia.uiDiscover.R
import xyz.stignarnia.uiDiscover.databinding.ViewDiscoverFiltersGenresBinding
import xyz.stignarnia.uiDiscover.filters.genres.DiscoverFiltersGenresUiEvent.ApplyFilters
import xyz.stignarnia.uiDiscover.filters.genres.DiscoverFiltersGenresUiEvent.CloseFilters
import xyz.stignarnia.uiModel.Genre

@AndroidEntryPoint
internal class DiscoverFiltersGenresBottomSheet : BaseBottomSheetFragment(R.layout.view_discover_filters_genres) {
  private val viewModel by viewModels<DiscoverFiltersGenresViewModel>()
  private val binding by viewBinding(ViewDiscoverFiltersGenresBinding::bind)

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
      applyButton.onClick { saveGenres() }
      clearButton.onClick { renderGenres(emptyList()) }
    }
  }

  private fun saveGenres() {
    with(binding) {
      val genres =
        mutableListOf<Genre>().apply {
          genresChipGroup.forEach { chip ->
            if ((chip as Chip).isChecked) {
              add(Genre.valueOf(chip.tag.toString()))
            }
          }
        }
      viewModel.saveGenres(genres)
    }
  }

  private fun render(uiState: DiscoverFiltersGenresUiState) {
    with(uiState) {
      genres?.let { renderGenres(it) }
    }
  }

  private fun renderGenres(genres: List<Genre>) {
    binding.genresChipGroup.removeAllViews()
    binding.clearButton.visibleIf(genres.isNotEmpty())

    val genresNames = genres.map { it.name }
    Genre
      .values()
      .sortedBy { requireContext().getString(it.displayName) }
      .forEach { genre ->
        val chip =
          Chip(requireContext()).apply {
            tag = genre.name
            text = requireContext().getString(genre.displayName)
            isCheckable = true
            isCheckedIconVisible = false
            setEnsureMinTouchTargetSize(false)
            shapeAppearanceModel =
              shapeAppearanceModel
                .toBuilder()
                .setAllCornerSizes(100f)
                .build()
            chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.selector_discover_chip_background)
            setChipStrokeColorResource(R.color.selector_discover_chip_stroke)
            setChipStrokeWidthResource(R.dimen.discoverFilterChipStroke)
            setTextColor(ContextCompat.getColorStateList(context, R.color.selector_discover_chip_text))
            isChecked = genre.name in genresNames
          }
        binding.genresChipGroup.addView(chip)
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
