package xyz.stignarnia.ui_my_shows.hidden

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
import xyz.stignarnia.common.Config.LISTS_GRID_SPAN
import xyz.stignarnia.repository.settings.SettingsViewModeRepository
import xyz.stignarnia.ui_base.BaseFragment
import xyz.stignarnia.ui_base.common.ListViewMode.LIST_NORMAL
import xyz.stignarnia.ui_base.common.OnScrollResetListener
import xyz.stignarnia.ui_base.common.OnSearchClickListener
import xyz.stignarnia.ui_base.common.sheets.sort_order.SortOrderBottomSheet
import xyz.stignarnia.ui_base.utilities.extensions.dimenToPx
import xyz.stignarnia.ui_base.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.ui_base.utilities.extensions.fadeIf
import xyz.stignarnia.ui_base.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.ui_base.utilities.extensions.navigateToSafe
import xyz.stignarnia.ui_base.utilities.extensions.requireSerializable
import xyz.stignarnia.ui_base.utilities.extensions.withSpanSizeLookup
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortOrder.DATE_ADDED
import xyz.stignarnia.ui_model.SortOrder.NAME
import xyz.stignarnia.ui_model.SortOrder.NEWEST
import xyz.stignarnia.ui_model.SortOrder.RATING
import xyz.stignarnia.ui_model.SortOrder.USER_RATING
import xyz.stignarnia.ui_model.SortType
import xyz.stignarnia.ui_my_shows.R
import xyz.stignarnia.ui_my_shows.common.filters.CollectionFiltersOrigin.HIDDEN_SHOWS
import xyz.stignarnia.ui_my_shows.common.filters.genre.CollectionFiltersGenreBottomSheet
import xyz.stignarnia.ui_my_shows.common.filters.genre.CollectionFiltersGenreBottomSheet.Companion.REQUEST_COLLECTION_FILTERS_GENRE
import xyz.stignarnia.ui_my_shows.common.filters.network.CollectionFiltersNetworkBottomSheet
import xyz.stignarnia.ui_my_shows.common.filters.network.CollectionFiltersNetworkBottomSheet.Companion.REQUEST_COLLECTION_FILTERS_NETWORK
import xyz.stignarnia.ui_my_shows.common.layout.CollectionShowLayoutManagerProvider
import xyz.stignarnia.ui_my_shows.common.layout.CollectionShowListItemDecoration
import xyz.stignarnia.ui_my_shows.common.recycler.CollectionAdapter
import xyz.stignarnia.ui_my_shows.common.recycler.CollectionListItem.FiltersItem
import xyz.stignarnia.ui_my_shows.common.recycler.CollectionListItem.ShowItem
import xyz.stignarnia.ui_my_shows.databinding.FragmentHiddenBinding
import xyz.stignarnia.ui_my_shows.main.FollowedShowsFragment
import xyz.stignarnia.ui_my_shows.main.FollowedShowsViewModel
import xyz.stignarnia.ui_navigation.java.NavigationArgs.ARG_SELECTED_SORT_ORDER
import xyz.stignarnia.ui_navigation.java.NavigationArgs.ARG_SELECTED_SORT_TYPE
import xyz.stignarnia.ui_navigation.java.NavigationArgs.REQUEST_SORT_ORDER
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HiddenFragment :
  BaseFragment<HiddenViewModel>(R.layout.fragment_hidden),
  OnScrollResetListener,
  OnSearchClickListener {

  @Inject lateinit var settings: SettingsViewModeRepository

  override val navigationId = R.id.followedShowsFragment
  private val binding by viewBinding(FragmentHiddenBinding::bind)

  private val parentViewModel by viewModels<FollowedShowsViewModel>({ requireParentFragment() })
  override val viewModel by viewModels<HiddenViewModel>()

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
    layoutManager = CollectionShowLayoutManagerProvider
      .provideLayoutManger(requireContext(), LIST_NORMAL, tabletGridSpanSize)
    adapter = CollectionAdapter(
      itemClickListener = { openShowDetails(it.show) },
      itemLongClickListener = { item -> openShowMenu(item.show) },
      sortChipClickListener = ::openSortOrderDialog,
      missingImageListener = viewModel::loadMissingImage,
      missingTranslationListener = viewModel::loadMissingTranslation,
      networksChipClickListener = ::openNetworksDialog,
      genresChipClickListener = ::openGenresDialog,
      upcomingChipClickListener = {},
      listChangeListener = {
        binding.hiddenRecycler.scrollToPosition(0)
        (requireParentFragment() as FollowedShowsFragment).resetTranslations()
      },
      upcomingChipVisible = false,
    ).apply {
      stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }
    binding.hiddenRecycler.apply {
      setHasFixedSize(true)
      adapter = this@HiddenFragment.adapter
      layoutManager = this@HiddenFragment.layoutManager
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
      addItemDecoration(CollectionShowListItemDecoration(requireContext(), R.dimen.spaceSmall))
    }
  }

  private fun setupInsets() {
    with(binding) {
      root.doOnApplyWindowInsets { _, insets, padding, _ ->
        val tabletOffset = if (isTablet) dimenToPx(R.dimen.spaceMedium) else 0
        val systemInset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        hiddenContent.updatePadding(top = padding.top + systemInset.top + tabletOffset)
        hiddenRecycler.updatePadding(
          top = dimenToPx(R.dimen.archiveTabsViewPadding),
          bottom = dimenToPx(R.dimen.myShowsBottomPadding) + systemInset.bottom,
        )
      }
    }
  }

  private fun render(uiState: HiddenUiState) {
    uiState.run {
      with(binding) {
        viewMode.let {
          if (adapter?.listViewMode != it) {
            layoutManager = CollectionShowLayoutManagerProvider
              .provideLayoutManger(requireContext(), it, tabletGridSpanSize)
            adapter?.listViewMode = it
            hiddenRecycler.let { recycler ->
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
          hiddenEmptyView.root.fadeIf(it.isEmpty() && !isSearching)
        }
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
    val options = listOf(NAME, RATING, USER_RATING, NEWEST, DATE_ADDED)
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

    val bundle = CollectionFiltersNetworkBottomSheet.createBundle(HIDDEN_SHOWS)
    navigateToSafe(R.id.actionFollowedShowsFragmentToNetworks, bundle)
  }

  private fun openGenresDialog() {
    requireParentFragment().setFragmentResultListener(REQUEST_COLLECTION_FILTERS_GENRE) { _, _ ->
      viewModel.loadShows(resetScroll = true)
    }

    val bundle = CollectionFiltersGenreBottomSheet.createBundle(HIDDEN_SHOWS)
    navigateToSafe(R.id.actionFollowedShowsFragmentToGenres, bundle)
  }

  override fun onEnterSearch() {
    isSearching = true
    with(binding) {
      hiddenRecycler.translationY = dimenToPx(R.dimen.myShowsSearchLocalOffset).toFloat()
      hiddenRecycler.smoothScrollToPosition(0)
    }
  }

  override fun onExitSearch() {
    isSearching = false
    with(binding.hiddenRecycler) {
      translationY = 0F
      postDelayed(200) { layoutManager?.scrollToPosition(0) }
    }
  }

  override fun onScrollReset() = binding.hiddenRecycler.scrollToPosition(0)

  override fun setupBackPressed() = Unit

  override fun onDestroyView() {
    adapter = null
    layoutManager = null
    super.onDestroyView()
  }
}
