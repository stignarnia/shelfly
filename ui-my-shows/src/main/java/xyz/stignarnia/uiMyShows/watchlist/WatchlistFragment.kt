package xyz.stignarnia.uiMyShows.watchlist

import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.postDelayed
import androidx.core.view.updatePadding
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.common.Config.LISTS_GRID_SPAN
import xyz.stignarnia.repository.settings.SettingsViewModeRepository
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.common.ListViewMode.LIST_NORMAL
import xyz.stignarnia.uiBase.common.OnScrollResetListener
import xyz.stignarnia.uiBase.common.OnSearchClickListener
import xyz.stignarnia.uiBase.common.sheets.sortOrder.SortOrderBottomSheet
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.uiBase.utilities.extensions.fadeIf
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.navigateToSafe
import xyz.stignarnia.uiBase.utilities.extensions.requireSerializable
import xyz.stignarnia.uiBase.utilities.extensions.withSpanSizeLookup
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortOrder.DATE_ADDED
import xyz.stignarnia.uiModel.SortOrder.NAME
import xyz.stignarnia.uiModel.SortOrder.NEWEST
import xyz.stignarnia.uiModel.SortOrder.RANDOM
import xyz.stignarnia.uiModel.SortOrder.RATING
import xyz.stignarnia.uiModel.SortOrder.USER_RATING
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiMyShows.R
import xyz.stignarnia.uiMyShows.common.filters.CollectionFiltersOrigin.WATCHLIST_SHOWS
import xyz.stignarnia.uiMyShows.common.filters.genre.CollectionFiltersGenreBottomSheet
import xyz.stignarnia.uiMyShows.common.filters.genre.CollectionFiltersGenreBottomSheet.Companion.REQUEST_COLLECTION_FILTERS_GENRE
import xyz.stignarnia.uiMyShows.common.filters.network.CollectionFiltersNetworkBottomSheet
import xyz.stignarnia.uiMyShows.common.filters.network.CollectionFiltersNetworkBottomSheet.Companion.REQUEST_COLLECTION_FILTERS_NETWORK
import xyz.stignarnia.uiMyShows.common.layout.CollectionShowLayoutManagerProvider
import xyz.stignarnia.uiMyShows.common.layout.CollectionShowListItemDecoration
import xyz.stignarnia.uiMyShows.common.recycler.CollectionAdapter
import xyz.stignarnia.uiMyShows.common.recycler.CollectionListItem.FiltersItem
import xyz.stignarnia.uiMyShows.common.recycler.CollectionListItem.ShowItem
import xyz.stignarnia.uiMyShows.databinding.FragmentWatchlistBinding
import xyz.stignarnia.uiMyShows.main.FollowedShowsFragment
import xyz.stignarnia.uiMyShows.main.FollowedShowsViewModel
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_SELECTED_SORT_ORDER
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_SELECTED_SORT_TYPE
import xyz.stignarnia.uiNavigation.java.NavigationArgs.REQUEST_SORT_ORDER
import javax.inject.Inject

@AndroidEntryPoint
class WatchlistFragment :
  BaseFragment<WatchlistViewModel>(R.layout.fragment_watchlist),
  OnScrollResetListener,
  OnSearchClickListener {
  @Inject lateinit var settings: SettingsViewModeRepository

  override val navigationId = R.id.followedShowsFragment
  private val binding by viewBinding(FragmentWatchlistBinding::bind)

  private val parentViewModel by viewModels<FollowedShowsViewModel>({ requireParentFragment() })
  override val viewModel by viewModels<WatchlistViewModel>()

  private var adapter: CollectionAdapter? = null
  private var layoutManager: LayoutManager? = null
  private var isSearching = false
  private val tabletGridSpanSize by lazy { settings.tabletGridSpanSize }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupInsets()
    setupRecycler()

    launchAndRepeatStarted(
      { parentViewModel.uiState.collect { viewModel.onParentState(it) } },
      { viewModel.uiState.collect { render(it) } },
      doAfterLaunch = { viewModel.loadShows() },
    )
  }

  private fun setupRecycler() {
    layoutManager =
      CollectionShowLayoutManagerProvider
        .provideLayoutManger(requireContext(), LIST_NORMAL, tabletGridSpanSize)
    adapter =
      CollectionAdapter(
        itemClickListener = { openShowDetails(it.show) },
        itemLongClickListener = { item -> openShowMenu(item.show) },
        sortChipClickListener = ::openSortOrderDialog,
        upcomingChipClickListener = viewModel::toggleUpcomingFilter,
        networksChipClickListener = ::openNetworksDialog,
        genresChipClickListener = ::openGenresDialog,
        missingImageListener = viewModel::loadMissingImage,
        missingTranslationListener = viewModel::loadMissingTranslation,
        listChangeListener = {
          binding.watchlistRecycler.scrollToPosition(0)
          (requireParentFragment() as FollowedShowsFragment).resetTranslations()
        },
      ).apply {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
      }
    binding.watchlistRecycler.apply {
      setHasFixedSize(true)
      adapter = this@WatchlistFragment.adapter
      layoutManager = this@WatchlistFragment.layoutManager
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
      addItemDecoration(CollectionShowListItemDecoration(requireContext(), R.dimen.spaceSmall))
    }
  }

  private fun setupInsets() {
    with(binding) {
      root.doOnApplyWindowInsets { _, insets, padding, _ ->
        val tabletOffset = if (isTablet) dimenToPx(R.dimen.spaceMedium) else 0
        val inset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        watchlistContent.updatePadding(top = padding.top + inset.top + tabletOffset)
        watchlistRecycler.updatePadding(
          top = dimenToPx(R.dimen.collectionTabsViewPadding),
          bottom = dimenToPx(R.dimen.myShowsBottomPadding) + inset.bottom,
        )
      }
    }
  }

  private fun render(uiState: WatchlistUiState) {
    uiState.run {
      viewMode.let {
        if (adapter?.listViewMode != it) {
          layoutManager =
            CollectionShowLayoutManagerProvider
              .provideLayoutManger(requireContext(), it, tabletGridSpanSize)
          adapter?.listViewMode = it
          binding.watchlistRecycler.let { recycler ->
            recycler.layoutManager = layoutManager
            recycler.adapter = adapter
          }
        }
      }
      items.let {
        val notifyChange = resetScroll?.consume() == true
        adapter?.setItems(it, notifyChange = notifyChange)
        (layoutManager as? GridLayoutManager)?.withSpanSizeLookup { pos ->
          when (adapter?.getItems()?.get(pos)) {
            is FiltersItem -> {
              when (viewMode) {
                LIST_NORMAL -> if (isTablet) tabletGridSpanSize else LISTS_GRID_SPAN
              }
            }

            is ShowItem -> {
              1
            }

            else -> {
              throw Error("Unsupported span size!")
            }
          }
        }
        binding.watchlistEmptyView.root.fadeIf(it.isEmpty() && !isSearching)
      }
      sortOrder?.let { event ->
        event.consume()?.let { openSortOrderDialog(it.first, it.second) }
      }
    }
  }

  private fun openShowDetails(show: Show) {
    (requireParentFragment() as? FollowedShowsFragment)?.openShowDetails(show)
  }

  private fun openShowMenu(show: Show) {
    (requireParentFragment() as? FollowedShowsFragment)?.openShowMenu(show)
  }

  private fun openSortOrderDialog(
    order: SortOrder,
    type: SortType,
  ) {
    val options = listOf(NAME, RATING, USER_RATING, NEWEST, DATE_ADDED, RANDOM)
    val args = SortOrderBottomSheet.createBundle(options, order, type)

    requireParentFragment().setFragmentResultListener(REQUEST_SORT_ORDER) { _, bundle ->
      val sortOrder = bundle.requireSerializable<SortOrder>(ARG_SELECTED_SORT_ORDER)
      val sortType = bundle.requireSerializable<SortType>(ARG_SELECTED_SORT_TYPE)
      viewModel.setSortOrder(sortOrder, sortType)
    }

    navigateTo(R.id.actionFollowedShowsFragmentToSortOrder, args)
  }

  private fun openNetworksDialog() {
    requireParentFragment().setFragmentResultListener(REQUEST_COLLECTION_FILTERS_NETWORK) { _, _ ->
      viewModel.loadShows(resetScroll = true)
    }

    val bundle = CollectionFiltersNetworkBottomSheet.createBundle(WATCHLIST_SHOWS)
    navigateToSafe(R.id.actionFollowedShowsFragmentToNetworks, bundle)
  }

  private fun openGenresDialog() {
    requireParentFragment().setFragmentResultListener(REQUEST_COLLECTION_FILTERS_GENRE) { _, _ ->
      viewModel.loadShows(resetScroll = true)
    }

    val bundle = CollectionFiltersGenreBottomSheet.createBundle(WATCHLIST_SHOWS)
    navigateToSafe(R.id.actionFollowedShowsFragmentToGenres, bundle)
  }

  override fun onEnterSearch() {
    isSearching = true
    with(binding) {
      watchlistRecycler.translationY = dimenToPx(R.dimen.myShowsSearchLocalOffset).toFloat()
      watchlistRecycler.smoothScrollToPosition(0)
    }
  }

  override fun onExitSearch() {
    isSearching = false
    with(binding.watchlistRecycler) {
      translationY = 0F
      postDelayed(200) { layoutManager?.scrollToPosition(0) }
    }
  }

  override fun onScrollReset() = binding.watchlistRecycler.scrollToPosition(0)

  override fun setupBackPressed() = Unit

  override fun onDestroyView() {
    adapter = null
    layoutManager = null
    super.onDestroyView()
  }
}
