package xyz.stignarnia.uiProgress.progress

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.fragment.app.setFragmentResultListener
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
import xyz.stignarnia.uiBase.common.WidgetsProvider
import xyz.stignarnia.uiBase.common.sheets.sortOrder.SortOrderBottomSheet
import xyz.stignarnia.uiBase.utilities.NavigationHost
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiBase.utilities.events.MessageEvent
import xyz.stignarnia.uiBase.utilities.extensions.add
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.uiBase.utilities.extensions.fadeIf
import xyz.stignarnia.uiBase.utilities.extensions.fadeIn
import xyz.stignarnia.uiBase.utilities.extensions.gone
import xyz.stignarnia.uiBase.utilities.extensions.navigateToSafe
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.requireSerializable
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.extensions.withSpanSizeLookup
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiModel.ProgressDateSelectionType.ALWAYS_ASK
import xyz.stignarnia.uiModel.ProgressDateSelectionType.NOW
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortOrder.EPISODES_LEFT
import xyz.stignarnia.uiModel.SortOrder.NAME
import xyz.stignarnia.uiModel.SortOrder.NEWEST
import xyz.stignarnia.uiModel.SortOrder.RANDOM
import xyz.stignarnia.uiModel.SortOrder.RATING
import xyz.stignarnia.uiModel.SortOrder.RECENTLY_WATCHED
import xyz.stignarnia.uiModel.SortOrder.USER_RATING
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiModel.Tip
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_SELECTED_NEW_AT_TOP
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_SELECTED_SORT_ORDER
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_SELECTED_SORT_TYPE
import xyz.stignarnia.uiNavigation.java.NavigationArgs.REQUEST_SORT_ORDER
import xyz.stignarnia.uiProgress.R
import xyz.stignarnia.uiProgress.databinding.FragmentProgressBinding
import xyz.stignarnia.uiProgress.helpers.ProgressLayoutManagerProvider
import xyz.stignarnia.uiProgress.main.EpisodeCheckActionUiEvent
import xyz.stignarnia.uiProgress.main.ProgressMainFragment
import xyz.stignarnia.uiProgress.main.ProgressMainViewModel
import xyz.stignarnia.uiProgress.main.RequestWidgetsUpdate
import xyz.stignarnia.uiProgress.progress.recycler.ProgressAdapter
import xyz.stignarnia.uiProgress.progress.recycler.ProgressListItem
import javax.inject.Inject

@AndroidEntryPoint
class ProgressFragment :
  BaseFragment<ProgressViewModel>(R.layout.fragment_progress),
  OnSearchClickListener,
  OnScrollResetListener {
  private companion object {
  }

  @Inject lateinit var settings: SettingsViewModeRepository

  override val navigationId = R.id.progressMainFragment
  private val binding by viewBinding(FragmentProgressBinding::bind)

  private val parentViewModel by viewModels<ProgressMainViewModel>({ requireParentFragment() })
  override val viewModel by viewModels<ProgressViewModel>()

  private var adapter: ProgressAdapter? = null
  private var layoutManager: LayoutManager? = null
  private var statusBarHeight = 0
  private var isSearching = false

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    setupRecycler()
    setupInsets()

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        with(parentViewModel) {
          launch { uiState.collect { viewModel.onParentState(it) } }
        }
        with(viewModel) {
          launch { uiState.collect { render(it) } }
          launch { backupProgress.collect { binding.progressOverscroll.setRunningProgress(it) } }
          launch { messageFlow.collect { showSnack(it) } }
          launch { eventFlow.collect { handleEvent(it) } }
        }
      }
    }
  }

  private fun setupView() {
    with(binding) {
      progressEmptyView.progressEmptyDiscoverButton.onClick {
        (requireActivity() as NavigationHost).navigateToDiscover()
      }
      progressTipItem.onClick {
        it.gone()
        showTip(Tip.WATCHLIST_ITEM_PIN)
      }
    }
  }

  private fun setupRecycler() {
    val gridSpanSize = settings.tabletGridSpanSize
    layoutManager = ProgressLayoutManagerProvider.provideLayoutManger(requireContext(), gridSpanSize)
    (layoutManager as? GridLayoutManager)?.run {
      withSpanSizeLookup { position ->
        when (adapter?.getItems()?.get(position)) {
          is ProgressListItem.Header -> gridSpanSize
          is ProgressListItem.Filters -> gridSpanSize
          is ProgressListItem.Episode -> 1
          else -> throw IllegalStateException()
        }
      }
    }
    adapter =
      ProgressAdapter(
        itemClickListener = { requireMainFragment().openShowDetails(it.show) },
        itemLongClickListener = { requireMainFragment().openShowMenu(it.show) },
        headerClickListener = { viewModel.toggleHeaderCollapsed(it.type) },
        detailsClickListener = {
          requireMainFragment().openEpisodeDetails(
            show = it.show,
            episode = it.requireEpisode(),
            season = it.requireSeason(),
          )
        },
        checkClickListener = viewModel::onEpisodeChecked,
        sortChipClickListener = viewModel::loadSortOrder,
        upcomingChipClickListener = viewModel::setUpcomingFilter,
        onHoldChipClickListener = viewModel::setOnHoldFilter,
        missingTranslationListener = viewModel::findMissingTranslation,
        missingImageListener = { item: ProgressListItem, force -> viewModel.findMissingImage(item, force) },
        listChangeListener = {
          requireMainFragment().resetTranslations()
          layoutManager?.scrollToPosition(0)
        },
      )
    binding.progressRecycler.apply {
      adapter = this@ProgressFragment.adapter
      layoutManager = this@ProgressFragment.layoutManager
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
      setHasFixedSize(true)
    }
  }

  private fun setupInsets() {
    with(binding) {
      val recyclerPadding =
        if (moviesEnabled) R.dimen.progressTabsViewPadding else R.dimen.progressTabsViewPaddingNoModes

      val overscrollPadding =
        if (moviesEnabled) R.dimen.progressOverscrollPadding else R.dimen.progressOverscrollPaddingNoModes

      if (statusBarHeight != 0) {
        (progressOverscroll.layoutParams as ViewGroup.MarginLayoutParams)
          .updateMargins(top = statusBarHeight + dimenToPx(overscrollPadding))
      }

      root.doOnApplyWindowInsets { _, insets, padding, _ ->
        val tabletOffset = if (isTablet) dimenToPx(R.dimen.spaceMedium) else 0
        val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        statusBarHeight = systemInsets.top + tabletOffset

        progressRecycler.updatePadding(
          top = statusBarHeight + dimenToPx(recyclerPadding),
          bottom = systemInsets.bottom + dimenToPx(R.dimen.bottomNavigationHeightPadded),
        )

        (progressEmptyView.root.layoutParams as ViewGroup.MarginLayoutParams)
          .updateMargins(top = statusBarHeight + dimenToPx(R.dimen.spaceBig))

        (progressOverscroll.layoutParams as ViewGroup.MarginLayoutParams)
          .updateMargins(top = statusBarHeight + dimenToPx(overscrollPadding))
      }
    }
  }

  private fun setupOverscroll() {
    if (view == null) return
    with(binding.progressOverscroll) {
      onTriggered = { onOverscrollTriggered() }
      attach(binding.progressRecycler, viewLifecycleOwner)
    }
  }

  /**
   * The pull completed.
   * Runs a backup, and a sync with the user's other devices, when one can actually run - and says so either way, because a gesture that animates and then does nothing is worse than no gesture.
   */
  private fun onOverscrollTriggered() {
    val started = viewModel.startBackupNow()
    val message = if (started) R.string.textBackupStarted else R.string.textBackupNotConfigured
    showSnack(MessageEvent.Info(message))
  }

  private fun openSortOrderDialog(
    order: SortOrder,
    type: SortType,
    newAtTop: Boolean,
  ) {
    val options = listOf(NAME, RATING, USER_RATING, NEWEST, RECENTLY_WATCHED, EPISODES_LEFT, RANDOM)
    val args = SortOrderBottomSheet.createBundle(options, order, type, newAtTop = Pair(true, newAtTop))

    requireParentFragment().setFragmentResultListener(REQUEST_SORT_ORDER) { _, bundle ->
      val sortOrder = bundle.requireSerializable<SortOrder>(ARG_SELECTED_SORT_ORDER)
      val sortType = bundle.requireSerializable<SortType>(ARG_SELECTED_SORT_TYPE)
      val newTop = bundle.getBoolean(ARG_SELECTED_NEW_AT_TOP)
      viewModel.setSortOrder(sortOrder, sortType, newTop)
    }

    navigateToSafe(R.id.actionProgressFragmentToSortOrder, args)
  }

  override fun onEnterSearch() {
    isSearching = true

    with(binding) {
      progressRecycler.translationY = dimenToPx(R.dimen.progressSearchLocalOffset).toFloat()
      progressRecycler.smoothScrollToPosition(0)
    }

    binding.progressOverscroll.detach()
  }

  override fun onExitSearch() {
    isSearching = false

    with(binding) {
      progressRecycler.translationY = 0F
      progressRecycler.smoothScrollToPosition(0)
    }

    setupOverscroll()
  }

  private fun handleEvent(event: Event<*>) {
    when (event) {
      is EpisodeCheckActionUiEvent -> {
        when (event.dateSelectionType) {
          ALWAYS_ASK -> requireMainFragment().openDateSelectionDialog(event.episode)
          NOW -> parentViewModel.setWatchedEpisode(event.episode)
        }
      }

      is RequestWidgetsUpdate -> {
        (requireAppContext() as WidgetsProvider).requestShowsWidgetsUpdate()
      }
    }
  }

  private fun render(uiState: ProgressUiState) {
    uiState.run {
      with(binding) {
        items?.let {
          val resetScroll = scrollReset?.consume() == true
          adapter?.setItems(it, resetScroll)
          renderFiltersEmpty(uiState)
          progressEmptyView.root.visibleIf(it.isEmpty() && !isLoading && !isSearching)
          progressTipItem.visibleIf(it.count() >= 3 && !isTipShown(Tip.WATCHLIST_ITEM_PIN))
          progressRecycler
            .fadeIn(
              duration = 200,
              withHardware = true,
            ).add(animations)
        }
      }
      isOverScrollEnabled.let {
        if (it) {
          setupOverscroll()
        } else {
          binding.progressOverscroll.detach()
        }
      }
      sortOrder?.let { event ->
        event.consume()?.let {
          openSortOrderDialog(it.first, it.second, it.third)
        }
      }
    }
  }

  private fun renderFiltersEmpty(uiState: ProgressUiState) {
    val items = uiState.items ?: emptyList()
    if (uiState.isLoading) {
      binding.progressEmptyFilterView.gone()
      return
    }
    if (isSearching) {
      binding.progressEmptyFilterView.fadeIf(items.isEmpty(), duration = 200)
      return
    }
    val hasActiveFilter = items.filterIsInstance<ProgressListItem.Filters>().firstOrNull()?.hasActiveFilters() == true
    val isFilterEmpty = hasActiveFilter && items.filterIsInstance<ProgressListItem.Episode>().isEmpty()
    binding.progressEmptyFilterView.fadeIf(isFilterEmpty, duration = 200)
  }

  override fun onScrollReset() {
    binding.progressRecycler.smoothScrollToPosition(0)
  }

  private fun requireMainFragment() = requireParentFragment() as ProgressMainFragment

  override fun setupBackPressed() = Unit

  override fun onDestroyView() {
    adapter = null
    layoutManager = null
    super.onDestroyView()
  }
}
