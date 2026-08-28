package xyz.stignarnia.uiMyShows.myshows

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
import xyz.stignarnia.uiModel.MyShowsSection
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortOrder.DATE_ADDED
import xyz.stignarnia.uiModel.SortOrder.NAME
import xyz.stignarnia.uiModel.SortOrder.NEWEST
import xyz.stignarnia.uiModel.SortOrder.RANDOM
import xyz.stignarnia.uiModel.SortOrder.RATING
import xyz.stignarnia.uiModel.SortOrder.RECENTLY_WATCHED
import xyz.stignarnia.uiModel.SortOrder.USER_RATING
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiMyShows.R
import xyz.stignarnia.uiMyShows.common.filters.CollectionFiltersOrigin.MY_SHOWS
import xyz.stignarnia.uiMyShows.common.filters.genre.CollectionFiltersGenreBottomSheet
import xyz.stignarnia.uiMyShows.common.filters.genre.CollectionFiltersGenreBottomSheet.Companion.REQUEST_COLLECTION_FILTERS_GENRE
import xyz.stignarnia.uiMyShows.common.filters.network.CollectionFiltersNetworkBottomSheet
import xyz.stignarnia.uiMyShows.common.filters.network.CollectionFiltersNetworkBottomSheet.Companion.REQUEST_COLLECTION_FILTERS_NETWORK
import xyz.stignarnia.uiMyShows.databinding.FragmentMyShowsBinding
import xyz.stignarnia.uiMyShows.main.FollowedShowsFragment
import xyz.stignarnia.uiMyShows.main.FollowedShowsViewModel
import xyz.stignarnia.uiMyShows.myshows.recycler.MyShowsAdapter
import xyz.stignarnia.uiMyShows.myshows.recycler.MyShowsItem
import xyz.stignarnia.uiMyShows.myshows.recycler.MyShowsItem.Type.ALL_SHOWS_HEADER
import xyz.stignarnia.uiMyShows.myshows.recycler.MyShowsItem.Type.ALL_SHOWS_ITEM
import xyz.stignarnia.uiMyShows.myshows.recycler.MyShowsItem.Type.RECENT_SHOWS
import xyz.stignarnia.uiMyShows.myshows.recycler.MyShowsLayoutManagerProvider
import xyz.stignarnia.uiMyShows.utilities.MyShowsListItemDecoration
import xyz.stignarnia.uiNavigation.java.NavigationArgs
import javax.inject.Inject

@AndroidEntryPoint
class MyShowsFragment :
  BaseFragment<MyShowsViewModel>(R.layout.fragment_my_shows),
  OnScrollResetListener,
  OnSearchClickListener {
  @Inject lateinit var settings: SettingsViewModeRepository

  override val navigationId = R.id.followedShowsFragment
  private val binding by viewBinding(FragmentMyShowsBinding::bind)

  private val parentViewModel by viewModels<FollowedShowsViewModel>({ requireParentFragment() })
  override val viewModel by viewModels<MyShowsViewModel>()

  private var adapter: MyShowsAdapter? = null
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
    layoutManager = MyShowsLayoutManagerProvider.provideLayoutManger(requireContext(), LIST_NORMAL, tabletGridSpanSize)
    adapter =
      MyShowsAdapter(
        itemClickListener = { openShowDetails(it.show) },
        itemLongClickListener = { item -> openShowMenu(item.show) },
        onSortOrderClickListener = { section, order, type -> openSortOrderDialog(section, order, type) },
        onTypeClickListener = { navigateToSafe(R.id.actionFollowedShowsFragmentToMyShowsFilters) },
        onNetworksClickListener = ::openNetworksDialog,
        onGenresClickListener = ::openGenresDialog,
        missingImageListener = { item, force -> viewModel.loadMissingImage(item as MyShowsItem, force) },
        missingTranslationListener = { viewModel.loadMissingTranslation(it as MyShowsItem) },
        listChangeListener = {
          layoutManager?.scrollToPosition(0)
          (requireParentFragment() as FollowedShowsFragment).resetTranslations()
        },
      ).apply {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
      }
    binding.myShowsRecycler.apply {
      adapter = this@MyShowsFragment.adapter
      layoutManager = this@MyShowsFragment.layoutManager
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
      setHasFixedSize(true)
      addItemDecoration(MyShowsListItemDecoration(requireContext(), R.dimen.spaceSmall))
    }
  }

  private fun setupInsets() {
    with(binding) {
      root.doOnApplyWindowInsets { view, insets, _, _ ->
        val tabletOffset = if (isTablet) dimenToPx(R.dimen.spaceMedium) else 0
        val systemInset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        myShowsRoot.updatePadding(top = systemInset.top + tabletOffset)
        myShowsRecycler.updatePadding(
          top = dimenToPx(R.dimen.myShowsTabsViewPadding),
          bottom = dimenToPx(R.dimen.myShowsBottomPadding) + systemInset.bottom,
        )
      }
    }
  }

  private fun render(uiState: MyShowsUiState) {
    uiState.run {
      with(binding) {
        viewMode.let {
          if (adapter?.listViewMode != it) {
            val state = myShowsRecycler.layoutManager?.onSaveInstanceState()
            layoutManager = MyShowsLayoutManagerProvider.provideLayoutManger(requireContext(), it, tabletGridSpanSize)
            adapter?.listViewMode = it
            myShowsRecycler.let { recycler ->
              recycler.layoutManager = layoutManager
              recycler.adapter = adapter
              recycler.layoutManager?.onRestoreInstanceState(state)
            }
          }
        }
        items?.let { items ->
          val notifyChangeList = resetScrollMap?.consume()
          adapter?.setItems(items, notifyChangeList)
          (layoutManager as? GridLayoutManager)?.withSpanSizeLookup { pos ->
            val item = adapter?.getItems()?.get(pos)
            when (item?.type) {
              RECENT_SHOWS, ALL_SHOWS_HEADER -> {
                when (viewMode) {
                  LIST_NORMAL -> if (isTablet) tabletGridSpanSize else LISTS_GRID_SPAN
                }
              }

              ALL_SHOWS_ITEM -> {
                1
              }

              null -> {
                throw Error("Unsupported span size!")
              }
            }
          }
          myShowsEmptyView.root.fadeIf(showEmptyView && !isSearching)
        }
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
    section: MyShowsSection,
    order: SortOrder,
    type: SortType,
  ) {
    val options = listOf(NAME, RATING, USER_RATING, NEWEST, DATE_ADDED, RECENTLY_WATCHED, RANDOM)
    val key = NavigationArgs.requestSortOrderSection(section.name)
    val args = SortOrderBottomSheet.createBundle(options, order, type, key)

    requireParentFragment().setFragmentResultListener(key) { requestKey, bundle ->
      val sortOrder = bundle.requireSerializable<SortOrder>(NavigationArgs.ARG_SELECTED_SORT_ORDER)
      val sortType = bundle.requireSerializable<SortType>(NavigationArgs.ARG_SELECTED_SORT_TYPE)
      MyShowsSection
        .values()
        .find { NavigationArgs.requestSortOrderSection(it.name) == requestKey }
        ?.let { viewModel.setSortOrder(sortOrder, sortType) }
    }

    navigateTo(R.id.actionFollowedShowsFragmentToSortOrder, args)
  }

  private fun openNetworksDialog() {
    requireParentFragment().setFragmentResultListener(REQUEST_COLLECTION_FILTERS_NETWORK) { _, _ ->
      viewModel.loadShows()
    }

    val bundle = CollectionFiltersNetworkBottomSheet.createBundle(MY_SHOWS)
    navigateToSafe(R.id.actionFollowedShowsFragmentToNetworks, bundle)
  }

  private fun openGenresDialog() {
    requireParentFragment().setFragmentResultListener(REQUEST_COLLECTION_FILTERS_GENRE) { _, _ ->
      viewModel.loadShows()
    }

    val bundle = CollectionFiltersGenreBottomSheet.createBundle(MY_SHOWS)
    navigateToSafe(R.id.actionFollowedShowsFragmentToGenres, bundle)
  }

  override fun onEnterSearch() {
    isSearching = true
    with(binding) {
      myShowsRecycler.translationY = dimenToPx(R.dimen.myShowsSearchLocalOffset).toFloat()
      myShowsRecycler.smoothScrollToPosition(0)
    }
  }

  override fun onExitSearch() {
    isSearching = false
    with(binding.myShowsRecycler) {
      translationY = 0F
      postDelayed(200) { layoutManager?.scrollToPosition(0) }
    }
  }

  override fun onScrollReset() = binding.myShowsRecycler.scrollToPosition(0)

  override fun setupBackPressed() = Unit

  override fun onDestroyView() {
    adapter = null
    layoutManager = null
    super.onDestroyView()
  }
}
