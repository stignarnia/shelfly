package xyz.stignarnia.uiProgress.progress

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
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
import xyz.stignarnia.repository.settings.SettingsWebDavRepository
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.common.OnScrollResetListener
import xyz.stignarnia.uiBase.common.OnSearchClickListener
import xyz.stignarnia.uiBase.common.WidgetsProvider
import xyz.stignarnia.uiBase.common.sheets.sortOrder.SortOrderBottomSheet
import xyz.stignarnia.uiBase.utilities.NavigationHost
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiBase.utilities.extensions.add
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.uiBase.utilities.extensions.fadeIf
import xyz.stignarnia.uiBase.utilities.extensions.fadeIn
import xyz.stignarnia.uiBase.utilities.extensions.followTranslationY
import xyz.stignarnia.uiBase.utilities.extensions.gone
import xyz.stignarnia.uiBase.utilities.extensions.navigateToSafe
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.requireSerializable
import xyz.stignarnia.uiBase.utilities.extensions.updatePaddingAndAnchorTop
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

  @Inject lateinit var webDavSettings: SettingsWebDavRepository

  override val navigationId = R.id.progressMainFragment
  private val binding by viewBinding(FragmentProgressBinding::bind)

  private val parentViewModel by viewModels<ProgressMainViewModel>({ requireParentFragment() })
  override val viewModel by viewModels<ProgressViewModel>()

  private var adapter: ProgressAdapter? = null
  private var layoutManager: LayoutManager? = null
  private var statusBarHeight = 0
  private var isSearching = false
  private var isTipWanted = false
  private val tipPositioner =
    ViewTreeObserver.OnPreDrawListener {
      positionTip()
      true
    }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    setupRecycler()
    setupInsets()
    setupTipPositioner()

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
      progressEmptyView.progressEmptySyncButton.run {
        visibleIf(webDavSettings.url.isNotBlank())
        onClick { requireMainFragment().openWebDavSync() }
      }
      progressTipItem.onClick {
        isTipWanted = false
        it.gone()
        showTip(Tip.WATCHLIST_ITEM_PIN)
      }
      progressFiltersView.run {
        onSortChipClicked = viewModel::loadSortOrder
        upcomingChipClicked = viewModel::setUpcomingFilter
        onHoldChipClicked = viewModel::setOnHoldFilter
        // The header tabs scroll away under a behaviour in the parent screen's layout, and the chips have to go with them.
        // While searching the chips also move down with the list, clear of the search field.
        followTranslationY(requireMainFragment().tabs) {
          if (isSearching) dimenToPx(R.dimen.progressSearchLocalOffset).toFloat() else 0F
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
          is ProgressListItem.Header -> gridSpanSize
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
        if (moviesEnabled) {
          R.dimen.progressTabsViewPadding
        } else {
          R.dimen.progressTabsViewPaddingNoModes
        }

      root.doOnApplyWindowInsets { _, insets, _, _ ->
        val tabletOffset = if (isTablet) dimenToPx(R.dimen.spaceMedium) else 0
        val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        statusBarHeight = systemInsets.top + tabletOffset

        // The chips sit where the list used to start, and the list now starts below them.
        val filtersTop = statusBarHeight + dimenToPx(recyclerPadding)
        (progressFiltersView.layoutParams as ViewGroup.MarginLayoutParams)
          .updateMargins(top = filtersTop)

        // The slot opens past the filter chips, taking the space out of the list's top padding so the content barely shifts.
        // The same sum Discover uses from its own chips, so the ring sits in the same place under them on both screens.
        progressOverscroll.openHeight =
          filtersTop +
          dimenToPx(R.dimen.chipHeight) +
          dimenToPx(R.dimen.discoverOverscrollGap) +
          dimenToPx(R.dimen.overscrollActionProgress) +
          dimenToPx(R.dimen.spaceMedium)
        // The gap under the header also has to clear the chips: their top padding, the chips themselves and their bottom padding.
        val listTopGap =
          filtersTop +
            dimenToPx(R.dimen.spaceSmall) +
            dimenToPx(R.dimen.chipHeight) +
            dimenToPx(R.dimen.progressFiltersPaddingBottom)
        progressOverscroll.restHeight = listTopGap
        // The list spans the whole window and carries the gap under the floating header as its own top padding, so an item scrolled past the gap slides under the header and off the top of the screen.
        // OverscrollRecyclerLayout moves it by the slot's extra height, leaving its resting position at the top of the window.
        progressRecycler.updatePaddingAndAnchorTop(
          top = listTopGap,
          bottom = systemInsets.bottom + dimenToPx(R.dimen.bottomNavigationHeightPadded),
        )

        (progressEmptyView.root.layoutParams as ViewGroup.MarginLayoutParams)
          .updateMargins(top = statusBarHeight + dimenToPx(R.dimen.spaceBig))
      }
    }
  }

  private fun setupOverscroll() {
    if (view == null) return
    with(binding.progressOverscroll) {
      onTriggered = { viewModel.startBackupNow() }
      attach(binding.progressRecycler, viewLifecycleOwner)
      // The chips are what the ring is placed under, so they are what it has to leave with when the header scrolls away.
      follow(binding.progressFiltersView)
    }
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
          progressEmptyView.root.visibleIf(it.isEmpty() && filters == null && !isLoading && !isSearching)
          isTipWanted = it.count() >= 2 && !isTipShown(Tip.WATCHLIST_ITEM_PIN)
          positionTip()
          progressRecycler
            .fadeIn(
              duration = 200,
              withHardware = true,
            ).add(animations)
        }
        progressFiltersView.visibleIf(filters != null)
        filters?.let { progressFiltersView.bind(it) }
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
    val hasActiveFilter = uiState.filters?.hasActiveFilters() == true
    val isFilterEmpty = hasActiveFilter && items.filterIsInstance<ProgressListItem.Episode>().isEmpty()
    binding.progressEmptyFilterView.fadeIf(isFilterEmpty, duration = 200)
  }

  /**
   * Registers the per-frame positioning only while the view is in the window.
   * A detached view hands out its own observer, which is merged into the window's on attach, so one registered in onViewCreated could not be removed again and outlived the view.
   */
  private fun setupTipPositioner() {
    binding.progressRoot.addOnAttachStateChangeListener(
      object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) = view.viewTreeObserver.addOnPreDrawListener(tipPositioner)

        override fun onViewDetachedFromWindow(view: View) = view.viewTreeObserver.removeOnPreDrawListener(tipPositioner)
      },
    )
  }

  /**
   * Puts the tip on the episode badge of the first show, and keeps it there through scrolling, the overscroll pull and item animations, since it runs before every frame.
   * It is hidden whenever that badge is not in the visible part of the list, rather than left floating over the chips or another row.
   */
  private fun positionTip() {
    if (view == null) return
    with(binding) {
      val index = adapter?.getItems()?.indexOfFirst { it is ProgressListItem.Episode } ?: -1
      val badge =
        progressRecycler
          .takeIf { isTipWanted && it.isVisible && index >= 0 }
          ?.findViewHolderForAdapterPosition(index)
          ?.itemView
          ?.findViewById<View>(R.id.progressItemSubtitle)

      val listLocation = IntArray(2).also { progressRecycler.getLocationInWindow(it) }
      val badgeLocation = IntArray(2).also { badge?.getLocationInWindow(it) }
      val listTop = listLocation[1] + progressRecycler.paddingTop
      val listBottom = listLocation[1] + progressRecycler.height - progressRecycler.paddingBottom
      val isBadgeShown = badge != null && badge.isShown && badgeLocation[1] >= listTop && badgeLocation[1] + badge.height <= listBottom
      progressTipItem.visibleIf(isBadgeShown)
      if (badge == null || !isBadgeShown) return

      val rootLocation = IntArray(2).also { progressRoot.getLocationInWindow(it) }
      val isRtl = progressRoot.layoutDirection == View.LAYOUT_DIRECTION_RTL
      val left = if (isRtl) badgeLocation[0] + badge.width - progressTipItem.width else badgeLocation[0]
      val top = badgeLocation[1] + (badge.height - progressTipItem.height) / 2
      progressTipItem.translationX = (left - rootLocation[0] - progressTipItem.left).toFloat()
      progressTipItem.translationY = (top - rootLocation[1] - progressTipItem.top).toFloat()
    }
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
