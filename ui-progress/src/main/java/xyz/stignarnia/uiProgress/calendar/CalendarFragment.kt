package xyz.stignarnia.uiProgress.calendar

import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import xyz.stignarnia.repository.settings.SettingsViewModeRepository
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.common.OnScrollResetListener
import xyz.stignarnia.uiBase.common.OnSearchClickListener
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.uiBase.utilities.extensions.fadeIn
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.extensions.withSpanSizeLookup
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiModel.CalendarMode.PRESENT_FUTURE
import xyz.stignarnia.uiModel.CalendarMode.RECENTS
import xyz.stignarnia.uiModel.ProgressDateSelectionType.ALWAYS_ASK
import xyz.stignarnia.uiModel.ProgressDateSelectionType.NOW
import xyz.stignarnia.uiProgress.R
import xyz.stignarnia.uiProgress.calendar.recycler.CalendarAdapter
import xyz.stignarnia.uiProgress.calendar.recycler.CalendarListItem
import xyz.stignarnia.uiProgress.databinding.FragmentCalendarBinding
import xyz.stignarnia.uiProgress.helpers.ProgressLayoutManagerProvider
import xyz.stignarnia.uiProgress.main.EpisodeCheckActionUiEvent
import xyz.stignarnia.uiProgress.main.ProgressMainFragment
import xyz.stignarnia.uiProgress.main.ProgressMainViewModel
import javax.inject.Inject

@AndroidEntryPoint
class CalendarFragment :
  BaseFragment<CalendarViewModel>(R.layout.fragment_calendar),
  OnSearchClickListener,
  OnScrollResetListener {
  @Inject lateinit var settings: SettingsViewModeRepository

  override val viewModel by viewModels<CalendarViewModel>()
  private val parentViewModel by viewModels<ProgressMainViewModel>({ requireParentFragment() })
  private val binding by viewBinding(FragmentCalendarBinding::bind)

  private var adapter: CalendarAdapter? = null
  private var layoutManager: LayoutManager? = null
  private var isSearching = false

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupRecycler()
    setupInsets()

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        with(parentViewModel) {
          launch { uiState.collect { viewModel.handleParentAction(it) } }
        }
        with(viewModel) {
          launch { uiState.collect { render(it) } }
          launch { messageFlow.collect { showSnack(it) } }
          launch { eventFlow.collect { handleEvent(it) } }
        }
      }
    }
  }

  private fun setupRecycler() {
    val gridSpanSize = settings.tabletGridSpanSize
    layoutManager = ProgressLayoutManagerProvider.provideLayoutManger(requireContext(), gridSpanSize)
    (layoutManager as? GridLayoutManager)?.run {
      withSpanSizeLookup { position ->
        when (adapter?.getItems()?.get(position)) {
          is CalendarListItem.Header -> gridSpanSize
          is CalendarListItem.Filters -> gridSpanSize
          is CalendarListItem.Episode -> 1
          else -> throw IllegalStateException()
        }
      }
    }
    adapter =
      CalendarAdapter(
        itemClickListener = { requireMainFragment().openShowDetails(it.show) },
        missingImageListener = { item, force -> viewModel.findMissingImage(item, force) },
        missingTranslationListener = { viewModel.findMissingTranslation(it) },
        modeClickListener = { requireMainFragment().toggleCalendarMode() },
        premieresClickListener = { viewModel.togglePremieresFilter() },
        checkClickListener = { viewModel.onEpisodeChecked(it) },
        detailsClickListener = {
          requireMainFragment().openEpisodeDetails(
            show = it.show,
            episode = it.episode,
            season = it.season,
          )
        },
      )
    binding.progressCalendarRecycler.apply {
      adapter = this@CalendarFragment.adapter
      layoutManager = this@CalendarFragment.layoutManager
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
      setHasFixedSize(true)
    }
  }

  private fun setupInsets() {
    val recyclerPadding =
      if (moviesEnabled) {
        R.dimen.progressCalendarTabsViewPadding
      } else {
        R.dimen.progressCalendarTabsViewPaddingNoModes
      }

    binding.progressCalendarRecycler.doOnApplyWindowInsets { view, insets, padding, _ ->
      val tabletOffset = if (isTablet) dimenToPx(R.dimen.spaceMedium) else 0
      val inset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      view.updatePadding(
        top = inset.top + tabletOffset + dimenToPx(recyclerPadding),
        bottom = inset.bottom + padding.bottom,
      )
    }
  }

  override fun onEnterSearch() {
    isSearching = true

    with(binding) {
      progressCalendarRecycler.translationY = dimenToPx(R.dimen.progressSearchLocalOffset).toFloat()
      progressCalendarRecycler.smoothScrollToPosition(0)
    }
  }

  override fun onExitSearch() {
    isSearching = false
    with(binding) {
      progressCalendarRecycler.translationY = 0F
      progressCalendarRecycler.smoothScrollToPosition(0)
    }
  }

  private fun handleEvent(event: Event<*>) {
    when (event) {
      is EpisodeCheckActionUiEvent -> {
        when (event.dateSelectionType) {
          ALWAYS_ASK -> requireMainFragment().openDateSelectionDialog(event.episode)
          NOW -> parentViewModel.setWatchedEpisode(event.episode)
        }
      }
    }
  }

  private fun render(uiState: CalendarUiState) {
    uiState.run {
      with(binding) {
        items?.let {
          adapter?.setItems(it)
          progressCalendarRecycler.fadeIn(150, withHardware = true)
          val anyEpisode = items.any { item -> item is CalendarListItem.Episode }
          progressCalendarEmptyFutureView.root.visibleIf(!anyEpisode && mode == PRESENT_FUTURE && !isSearching)
          progressCalendarEmptyRecentsView.root.visibleIf(!anyEpisode && mode == RECENTS && !isSearching)
        }
      }
    }
  }

  override fun onScrollReset() = binding.progressCalendarRecycler.smoothScrollToPosition(0)

  private fun requireMainFragment() = requireParentFragment() as ProgressMainFragment

  override fun onDestroyView() {
    adapter = null
    layoutManager = null
    super.onDestroyView()
  }

  override fun setupBackPressed() = Unit
}
