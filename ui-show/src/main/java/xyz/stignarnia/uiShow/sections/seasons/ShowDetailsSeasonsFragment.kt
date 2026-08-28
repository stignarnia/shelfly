package xyz.stignarnia.uiShow.sections.seasons

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.repository.settings.SettingsViewModeRepository
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.common.WidgetsProvider
import xyz.stignarnia.uiBase.common.sheets.dateSelection.DateSelectionBottomSheet
import xyz.stignarnia.uiBase.common.sheets.dateSelection.DateSelectionBottomSheet.Companion.REQUEST_DATE_SELECTION
import xyz.stignarnia.uiBase.common.sheets.dateSelection.DateSelectionBottomSheet.Companion.RESULT_DATE_SELECTION
import xyz.stignarnia.uiBase.common.sheets.dateSelection.DateSelectionBottomSheet.Result
import xyz.stignarnia.uiBase.utilities.events.MessageEvent
import xyz.stignarnia.uiBase.utilities.extensions.fadeIf
import xyz.stignarnia.uiBase.utilities.extensions.gone
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.navigateToSafe
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.requireParcelable
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiModel.Season
import xyz.stignarnia.uiShow.R
import xyz.stignarnia.uiShow.ShowDetailsViewModel
import xyz.stignarnia.uiShow.databinding.FragmentShowDetailsSeasonsBinding
import xyz.stignarnia.uiShow.episodes.ShowDetailsEpisodesFragment
import xyz.stignarnia.uiShow.quicksetup.QuickSetupListItem
import xyz.stignarnia.uiShow.quicksetup.QuickSetupView
import xyz.stignarnia.uiShow.sections.seasons.ShowDetailsSeasonsEvent.OpenQuickProgressDateSelection
import xyz.stignarnia.uiShow.sections.seasons.ShowDetailsSeasonsEvent.OpenSeasonDateSelection
import xyz.stignarnia.uiShow.sections.seasons.ShowDetailsSeasonsEvent.OpenSeasonEpisodes
import xyz.stignarnia.uiShow.sections.seasons.ShowDetailsSeasonsEvent.RequestWidgetsUpdate
import xyz.stignarnia.uiShow.sections.seasons.recycler.SeasonListItem
import xyz.stignarnia.uiShow.sections.seasons.recycler.SeasonsAdapter
import xyz.stignarnia.uiShow.sections.seasons.recycler.helpers.SeasonsGridItemDecoration
import xyz.stignarnia.uiShow.sections.seasons.recycler.helpers.SeasonsLayoutManagerProvider
import java.time.Duration
import javax.inject.Inject

@AndroidEntryPoint
class ShowDetailsSeasonsFragment : BaseFragment<ShowDetailsSeasonsViewModel>(R.layout.fragment_show_details_seasons) {
  @Inject lateinit var settings: SettingsViewModeRepository

  override val navigationId = R.id.showDetailsFragment
  private val binding by viewBinding(FragmentShowDetailsSeasonsBinding::bind)

  private val parentViewModel by viewModels<ShowDetailsViewModel>({ requireParentFragment() })
  override val viewModel by viewModels<ShowDetailsSeasonsViewModel>()

  private var seasonsAdapter: SeasonsAdapter? = null

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()

    launchAndRepeatStarted(
      { parentViewModel.parentEvents.collect { viewModel.handleEvent(it) } },
      { parentViewModel.parentShowState.collect { it?.let { viewModel.loadSeasons(it) } } },
      { viewModel.uiState.collect { render(it) } },
      { viewModel.eventFlow.collect { handleEvent(it as ShowDetailsSeasonsEvent<*>) } },
    )
  }

  override fun onResume() {
    viewModel.refreshSeasons()
    super.onResume()
  }

  private fun setupView() {
    seasonsAdapter =
      SeasonsAdapter(
        itemClickListener = { viewModel.openSeasonEpisodes(it) },
        itemCheckedListener = { item: SeasonListItem, isChecked: Boolean ->
          viewModel.onSeasonChecked(item.season, isChecked)
        },
      )
    binding.showDetailsSeasonsRecycler.apply {
      adapter = seasonsAdapter
      layoutManager = SeasonsLayoutManagerProvider.provideLayoutManger(requireContext(), settings)
      itemAnimator = null
      if (layoutManager is GridLayoutManager) {
        addItemDecoration(SeasonsGridItemDecoration(requireContext(), R.dimen.spaceBig))
      }
    }
    binding.showDetailsSeasonsLabel.text = getString(R.string.textSeasons).replace(":", "")
  }

  private fun render(uiState: ShowDetailsSeasonsUiState) {
    with(uiState) {
      seasons?.let {
        renderSeasons(it)
        renderRuntimeLeft(it)
      }
    }
  }

  private fun renderSeasons(seasonsItems: List<SeasonListItem>) {
    with(binding) {
      seasonsAdapter?.setItems(seasonsItems)
      showDetailsSeasonsProgress.gone()
      showDetailsSeasonsEmptyView.visibleIf(seasonsItems.isEmpty())
      showDetailsSeasonsRecycler.fadeIf(seasonsItems.isNotEmpty(), hardware = true)
      showDetailsSeasonsLabel.fadeIf(seasonsItems.isNotEmpty(), hardware = true)
      showDetailsQuickProgress.fadeIf(seasonsItems.isNotEmpty(), hardware = true)
      showDetailsQuickProgress.onClick {
        if (seasonsItems.any { !it.season.isSpecial() }) {
          openQuickSetupDialog(seasonsItems.map { it.season })
        } else {
          showSnack(MessageEvent.Info(R.string.textSeasonsEmpty))
        }
      }
    }
  }

  private fun renderRuntimeLeft(seasonsItems: List<SeasonListItem>) {
    val runtimeLeft =
      seasonsItems
        .filter { !it.season.isSpecial() }
        .flatMap { it.episodes }
        .filterNot { it.isWatched }
        .sumOf { it.episode.runtime }
        .toLong()

    val duration = Duration.ofMinutes(runtimeLeft)
    val hours = duration.toHours()
    val minutes = duration.minusHours(hours).toMinutes()

    val runtimeText =
      when {
        hours <= 0 -> getString(R.string.textRuntimeLeftMinutes, minutes.toString())
        else -> getString(R.string.textRuntimeLeftHours, hours.toString(), minutes.toString())
      }
    with(binding) {
      showDetailsRuntimeLeft.text = runtimeText
      showDetailsRuntimeLeft.fadeIf(seasonsItems.isNotEmpty() && runtimeLeft > 0, hardware = true)
    }
  }

  private fun handleEvent(event: ShowDetailsSeasonsEvent<*>) {
    when (event) {
      is OpenSeasonDateSelection -> {
        openDateSelectionDialog(event.season)
      }

      is OpenQuickProgressDateSelection -> {
        openDateSelectionDialog(event.item)
      }

      is OpenSeasonEpisodes -> {
        val bundle = ShowDetailsEpisodesFragment.createBundle(event.showId, event.seasonId)
        navigateToSafe(R.id.actionShowDetailsFragmentToEpisodes, bundle)
      }

      is RequestWidgetsUpdate -> {
        (requireAppContext() as WidgetsProvider).requestShowsWidgetsUpdate()
      }
    }
  }

  private fun openQuickSetupDialog(seasons: List<Season>) {
    val context = requireContext()
    val view =
      QuickSetupView(context).apply {
        bind(seasons)
      }
    modal()
      .setView(view)
      .setPositiveButton(R.string.textSelect) { modal ->
        viewModel.onQuickProgressSelected(view.getSelectedItem())
        modal.dismiss()
      }.setNegativeButton(R.string.textCancel)
      .show()
  }

  private fun openDateSelectionDialog(season: Season) {
    requireParentFragment().setFragmentResultListener(REQUEST_DATE_SELECTION) { _, bundle ->
      when (val result = bundle.requireParcelable<Result>(RESULT_DATE_SELECTION)) {
        is Result.Now -> viewModel.setSeasonWatched(season, true)
        is Result.CustomDate -> viewModel.setSeasonWatched(season, true, result.date)
        is Result.ReleaseDate -> viewModel.setSeasonWatched(season, true, result.date)
      }
    }
    val options = DateSelectionBottomSheet.createBundle(season.firstAired)
    navigateToSafe(R.id.actionShowDetailsFragmentToDateSelection, options)
  }

  private fun openDateSelectionDialog(item: QuickSetupListItem) {
    requireParentFragment().setFragmentResultListener(REQUEST_DATE_SELECTION) { _, bundle ->
      when (val result = bundle.requireParcelable<Result>(RESULT_DATE_SELECTION)) {
        is Result.Now -> viewModel.setQuickProgress(item, null)
        is Result.CustomDate -> viewModel.setQuickProgress(item, result.date)
        is Result.ReleaseDate -> viewModel.setQuickProgress(item, result.date)
      }
    }
    val options = DateSelectionBottomSheet.createBundle(item.episode.firstAired)
    navigateToSafe(R.id.actionShowDetailsFragmentToDateSelection, options)
  }

  override fun setupBackPressed() = Unit

  override fun onDestroyView() {
    seasonsAdapter = null
    super.onDestroyView()
  }
}
