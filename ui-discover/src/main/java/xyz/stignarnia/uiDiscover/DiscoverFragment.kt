package xyz.stignarnia.uiDiscover

import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.addCallback
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy
import androidx.recyclerview.widget.SimpleItemAnimator
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.common.OnTabReselectedListener
import xyz.stignarnia.uiBase.common.sheets.contextMenu.ContextMenuBottomSheet
import xyz.stignarnia.uiBase.utilities.extensions.add
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.disableUi
import xyz.stignarnia.uiBase.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.uiBase.utilities.extensions.enableUi
import xyz.stignarnia.uiBase.utilities.extensions.fadeIn
import xyz.stignarnia.uiBase.utilities.extensions.fadeOut
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.navigateToSafe
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.visible
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.extensions.withSpanSizeLookup
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiDiscover.databinding.FragmentDiscoverBinding
import xyz.stignarnia.uiDiscover.helpers.DiscoverLayoutManagerProvider
import xyz.stignarnia.uiDiscover.recycler.DiscoverAdapter
import xyz.stignarnia.uiDiscover.recycler.DiscoverListItem
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_SHOW_ID
import xyz.stignarnia.uiNavigation.java.NavigationArgs.REQUEST_ITEM_MENU
import kotlin.random.Random

@AndroidEntryPoint
internal class DiscoverFragment :
  BaseFragment<DiscoverViewModel>(R.layout.fragment_discover),
  OnTabReselectedListener {
  companion object {
    const val REQUEST_DISCOVER_FILTERS = "REQUEST_DISCOVER_FILTERS"
  }

  override val navigationId = R.id.discoverFragment

  override val viewModel by viewModels<DiscoverViewModel>()
  private val binding by viewBinding(FragmentDiscoverBinding::bind)

  private var adapter: DiscoverAdapter? = null
  private var layoutManager: GridLayoutManager? = null

  private var searchViewPosition = 0F
  private var tabsViewPosition = 0F
  private var filtersViewPosition = 0F

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    savedInstanceState?.let {
      searchViewPosition = it.getFloat("ARG_SEARCH_POS", 0F)
      tabsViewPosition = it.getFloat("ARG_TABS_POS", 0F)
      filtersViewPosition = it.getFloat("ARG_FILTERS_POS", 0F)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putFloat("ARG_SEARCH_POS", searchViewPosition)
    outState.putFloat("ARG_TABS_POS", tabsViewPosition)
    outState.putFloat("ARG_FILTERS_POS", filtersViewPosition)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    setupRecycler()
    setupOverscroll()
    setupInsets()

    launchAndRepeatStarted(
      { viewModel.uiState.collect { render(it) } },
      { viewModel.messageFlow.collect { showSnack(it) } },
      doAfterLaunch = { viewModel.loadShows() },
    )

    setFragmentResultListener(REQUEST_DISCOVER_FILTERS) { _, _ ->
      viewModel.loadShows(resetScroll = true, skipCache = true, instantProgress = true)
    }
  }

  override fun onResume() {
    super.onResume()
    showNavigation()
  }

  override fun onPause() {
    enableUi()
    with(binding) {
      searchViewPosition = discoverSearchView.translationY
      tabsViewPosition = discoverModeTabsView.translationY
      filtersViewPosition = discoverFiltersView.translationY
    }
    super.onPause()
  }

  override fun onDestroyView() {
    binding.discoverOverscroll.detach()
    adapter = null
    layoutManager = null
    super.onDestroyView()
  }

  private fun setupView() {
    with(binding) {
      discoverSearchView.run {
        settingsIconVisible = true
        isEnabled = false
        onClick { openSearch() }
        onSettingsClickListener = {
          hideNavigation()
          navigateToSafe(R.id.actionDiscoverFragmentToSettingsFragment)
        }
        translationY = searchViewPosition
      }
      discoverModeTabsView.run {
        visibleIf(moviesEnabled)
        translationY = tabsViewPosition
        onModeSelected = { mode = it }
        selectShows()
      }
      discoverFiltersView.run {
        translationY = filtersViewPosition
        onGenresChipClick = { navigateToSafe(R.id.actionDiscoverFragmentToFiltersGenres) }
        onProvidersChipClick = { navigateToSafe(R.id.actionDiscoverFragmentToFiltersProviders) }
        onFeedChipClick = { navigateToSafe(R.id.actionDiscoverFragmentToFiltersFeed) }
        onHideCollectionChipClick = { viewModel.toggleCollection() }
      }
    }
  }

  private fun setupRecycler() {
    layoutManager =
      DiscoverLayoutManagerProvider.provideLayoutManager(requireContext()).apply {
        withSpanSizeLookup { pos ->
          adapter
            ?.getItems()
            ?.getOrNull(pos)
            ?.image
            ?.type
            ?.getSpan(isTablet) ?: 1
        }
      }
    adapter =
      DiscoverAdapter(
        itemClickListener = { openDetails(it) },
        itemLongClickListener = { item -> openShowMenu(item.show) },
        missingImageListener = { ids, force -> viewModel.loadMissingImage(ids, force) },
        listChangeListener = { binding.discoverRecycler.scrollToPosition(0) },
      ).apply {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
      }
    binding.discoverRecycler.apply {
      adapter = this@DiscoverFragment.adapter
      layoutManager = this@DiscoverFragment.layoutManager
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
      setHasFixedSize(true)
    }
  }

  private fun setupOverscroll() {
    with(binding.discoverOverscroll) {
      onTriggered = {
        searchViewPosition = 0F
        tabsViewPosition = 0F
        viewModel.loadShows(pullToRefresh = true)
      }
      attach(binding.discoverRecycler, viewLifecycleOwner)
      // The chips are what the ring is placed under, so they are what it has to leave with when the header scrolls away.
      follow(binding.discoverFiltersView)
    }
  }

  private fun setupInsets() {
    with(binding) {
      discoverRoot.doOnApplyWindowInsets { _, insets, _, _ ->
        val tabletOffset = if (isTablet) dimenToPx(R.dimen.spaceMedium) else 0
        val statusBarSize = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top + tabletOffset

        val recyclerPadding =
          if (moviesEnabled) {
            R.dimen.discoverRecyclerPadding
          } else {
            R.dimen.discoverRecyclerPaddingNoTabs
          }

        val filtersPadding =
          if (moviesEnabled) {
            R.dimen.collectionFiltersMargin
          } else {
            R.dimen.collectionFiltersMarginNoTabs
          }

        // The slot opens past the filter chips, taking the space out of the list's top padding so the content barely shifts.
        // Derived from the same dimens that place the chips, so the ring keeps its gap under them on any screen size instead of trusting one number.
        discoverOverscroll.openHeight = statusBarSize +
          dimenToPx(filtersPadding) +
          dimenToPx(R.dimen.chipHeight) +
          dimenToPx(R.dimen.discoverOverscrollGap) +
          dimenToPx(R.dimen.overscrollActionProgress) +
          dimenToPx(R.dimen.spaceMedium)
        val listTopGap = statusBarSize + dimenToPx(recyclerPadding)
        discoverOverscroll.restHeight = listTopGap
        // The list spans the whole window and carries the gap under the floating header as its own top padding, so a poster scrolled past the gap slides under the header and off the top of the screen.
        // OverscrollRecyclerLayout moves it by the slot's extra height, leaving its resting position at the top of the window.
        val wasAtTop = !discoverRecycler.canScrollVertically(-1)
        discoverRecycler
          .updatePadding(top = listTopGap)
        if (wasAtTop) {
          (discoverRecycler.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(0, 0)
        }
        (discoverSearchView.layoutParams as MarginLayoutParams)
          .updateMargins(top = statusBarSize + dimenToPx(R.dimen.spaceMedium))
        (discoverModeTabsView.layoutParams as MarginLayoutParams)
          .updateMargins(top = statusBarSize + dimenToPx(R.dimen.collectionTabsMargin))
        (discoverFiltersView.layoutParams as MarginLayoutParams)
          .updateMargins(top = statusBarSize + dimenToPx(filtersPadding))
      }
    }
  }

  override fun setupBackPressed() {
    val dispatcher = requireActivity().onBackPressedDispatcher
    dispatcher.addCallback(viewLifecycleOwner) {
      isEnabled = false
      dispatcher.onBackPressed()
    }
  }

  private fun openSearch() {
    disableUi()
    hideNavigation()
    with(binding) {
      discoverModeTabsView.fadeOut(duration = 200).add(animations)
      discoverFiltersView.fadeOut(duration = 200).add(animations)
      discoverRecycler
        .fadeOut(duration = 200) {
          navigateToSafe(R.id.actionDiscoverFragmentToSearchFragment)
        }.add(animations)
    }
  }

  private fun openDetails(item: DiscoverListItem) {
    if (!binding.discoverRecycler.isEnabled) return
    disableUi()
    hideNavigation()
    animateItemsExit(item)
  }

  private fun openShowMenu(show: Show) {
    if (!binding.discoverRecycler.isEnabled) return
    setFragmentResultListener(REQUEST_ITEM_MENU) { requestKey, _ ->
      if (requestKey == REQUEST_ITEM_MENU) {
        viewModel.loadShows()
      }
      clearFragmentResultListener(REQUEST_ITEM_MENU)
    }
    val bundle = ContextMenuBottomSheet.createBundle(show.ids.tmdb)
    navigateToSafe(R.id.actionDiscoverFragmentToItemMenu, bundle)
  }

  private fun animateItemsExit(item: DiscoverListItem) {
    with(binding) {
      discoverSearchView.fadeOut().add(animations)
      discoverModeTabsView.fadeOut().add(animations)
      discoverFiltersView.fadeOut().add(animations)

      val clickedIndex = adapter?.indexOf(item) ?: 0
      val itemsCount = adapter?.itemCount ?: 0
      (0..itemsCount).forEach {
        if (it != clickedIndex) {
          val view = discoverRecycler.findViewHolderForAdapterPosition(it)
          view?.let { v ->
            val randomDelay = Random.nextLong(50, 200)
            v.itemView.fadeOut(duration = 150, startDelay = randomDelay).add(animations)
          }
        }
      }

      val clickedView = discoverRecycler.findViewHolderForAdapterPosition(clickedIndex)
      clickedView
        ?.itemView
        ?.fadeOut(
          duration = 150,
          startDelay = 350,
          endAction = {
            if (!isResumed) return@fadeOut
            val bundle = Bundle().apply { putLong(ARG_SHOW_ID, item.show.tmdbId) }
            navigateToSafe(R.id.actionDiscoverFragmentToShowDetailsFragment, bundle)
          },
        ).add(animations)
    }
  }

  private fun render(uiState: DiscoverUiState) {
    uiState.run {
      with(binding) {
        items?.let {
          val resetScroll = resetScroll?.consume() == true
          adapter?.setItems(it, resetScroll)
          discoverRecycler.fadeIn(200, withHardware = true)
        }
        isLoading?.let {
          discoverSearchView.isEnabled = !it
          discoverOverscroll.setRunning(it)
          discoverModeTabsView.isEnabled = !it
          discoverFiltersView.isEnabled = !it
          discoverRecycler.isEnabled = !it
        }
        filters?.let {
          if (discoverFiltersView.visibility != View.VISIBLE) {
            discoverFiltersView.visible()
          }
          discoverFiltersView.bind(it)
        }
      }
    }
  }

  override fun onTabReselected() = openSearch()
}
