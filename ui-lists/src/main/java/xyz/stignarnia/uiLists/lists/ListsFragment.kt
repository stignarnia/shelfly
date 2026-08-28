package xyz.stignarnia.uiLists.lists

import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.addCallback
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.postDelayed
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.repository.settings.SettingsViewModeRepository
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.common.OnTabReselectedListener
import xyz.stignarnia.uiBase.common.sheets.sortOrder.SortOrderBottomSheet
import xyz.stignarnia.uiBase.events.Event
import xyz.stignarnia.uiBase.events.EventsManager
import xyz.stignarnia.uiBase.utilities.ModeHost
import xyz.stignarnia.uiBase.utilities.extensions.add
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.disableUi
import xyz.stignarnia.uiBase.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.uiBase.utilities.extensions.enableUi
import xyz.stignarnia.uiBase.utilities.extensions.fadeIf
import xyz.stignarnia.uiBase.utilities.extensions.fadeIn
import xyz.stignarnia.uiBase.utilities.extensions.fadeOut
import xyz.stignarnia.uiBase.utilities.extensions.gone
import xyz.stignarnia.uiBase.utilities.extensions.hideKeyboard
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.navigateToSafe
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.requireSerializable
import xyz.stignarnia.uiBase.utilities.extensions.showKeyboard
import xyz.stignarnia.uiBase.utilities.extensions.updateTopMargin
import xyz.stignarnia.uiBase.utilities.extensions.visible
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiLists.R
import xyz.stignarnia.uiLists.databinding.FragmentListsBinding
import xyz.stignarnia.uiLists.lists.helpers.ListsLayoutManagerProvider
import xyz.stignarnia.uiLists.lists.recycler.ListsAdapter
import xyz.stignarnia.uiLists.lists.recycler.ListsItem
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortOrder.DATE_UPDATED
import xyz.stignarnia.uiModel.SortOrder.NAME
import xyz.stignarnia.uiModel.SortOrder.NEWEST
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiNavigation.java.NavigationArgs
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_LIST
import xyz.stignarnia.uiNavigation.java.NavigationArgs.REQUEST_CREATE_LIST
import javax.inject.Inject

@AndroidEntryPoint
class ListsFragment :
  BaseFragment<ListsViewModel>(R.layout.fragment_lists),
  OnTabReselectedListener {
  companion object {
    private const val TRANSLATION_DURATION = 225L
  }

  override val navigationId = R.id.listsFragment
  override val viewModel by viewModels<ListsViewModel>()
  private val binding by viewBinding(FragmentListsBinding::bind)

  @Inject lateinit var eventsManager: EventsManager

  @Inject lateinit var settings: SettingsViewModeRepository

  private var adapter: ListsAdapter? = null
  private var layoutManager: LayoutManager? = null

  private var searchViewTranslation = 0F
  private var tabsTranslation = 0F
  private var isFabHidden = false
  private var isSearching = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    savedInstanceState?.let {
      searchViewTranslation = it.getFloat("ARG_SEARCH_POSITION")
      tabsTranslation = it.getFloat("ARG_TABS_POSITION")
      isFabHidden = it.getBoolean("ARG_FAB_HIDDEN")
    }
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    setupInsets()
    setupRecycler()

    launchAndRepeatStarted(
      { viewModel.uiState.collect { render(it) } },
      { eventsManager.events.collect { handleEvent(it) } },
      doAfterLaunch = { viewModel.loadItems(resetScroll = false) },
    )
  }

  override fun onResume() {
    super.onResume()
    showNavigation()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putFloat("ARG_SEARCH_POSITION", searchViewTranslation)
    outState.putFloat("ARG_TABS_POSITION", tabsTranslation)
    outState.putBoolean("ARG_FAB_HIDDEN", isFabHidden)
  }

  override fun onPause() {
    enableUi()
    with(binding) {
      tabsTranslation = fragmentListsModeTabs.translationY
      searchViewTranslation = fragmentListsSearchView.translationY
      isFabHidden = fragmentListsCreateListButton.visibility != VISIBLE
    }
    super.onPause()
  }

  private fun setupView() {
    with(binding) {
      fragmentListsSearchView.run {
        hint = getString(R.string.textSearchFor)
        onSettingsClickListener = { openSettings() }
      }
      with(fragmentListsSearchLocalView) {
        onCloseClickListener = { exitSearch() }
      }
      fragmentListsModeTabs.run {
        onModeSelected = { (requireActivity() as ModeHost).setMode(it, force = true) }
        showMovies(moviesEnabled)
        showLists(true, anchorEnd = moviesEnabled)
        selectLists()
      }
      fragmentListsCreateListButton.run {
        if (!isFabHidden) fadeIn()
        onClick { openCreateList() }
      }
      fragmentListsFilters.onSortClickListener = { sortOrder, sortType ->
        showSortOrderDialog(sortOrder, sortType)
      }
      fragmentListsSearchButton.run {
        onClick { if (!isSearching) enterSearch() else exitSearch() }
      }
      fragmentListsSearchView.onClick { openMainSearch() }

      fragmentListsSearchView.translationY = searchViewTranslation
      fragmentListsModeTabs.translationY = tabsTranslation
      fragmentListsIcons.translationY = tabsTranslation
    }
  }

  private fun setupInsets() {
    with(binding) {
      fragmentListsRoot.doOnApplyWindowInsets { _, insets, _, _ ->
        val tabletOffset = if (isTablet) dimenToPx(R.dimen.spaceMedium) else 0
        val inset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val statusBarSize = inset.top + tabletOffset
        fragmentListsRecycler
          .updatePadding(
            top = statusBarSize + dimenToPx(R.dimen.listsRecyclerPaddingTop),
            bottom = inset.bottom + dimenToPx(R.dimen.listsBottomPadding),
          )
        fragmentListsSearchView.applyWindowInsetBehaviour(dimenToPx(R.dimen.spaceNormal) + statusBarSize)
        fragmentListsSearchView.updateTopMargin(dimenToPx(R.dimen.spaceMedium) + statusBarSize)
        fragmentListsModeTabs.updateTopMargin(dimenToPx(R.dimen.collectionTabsMargin) + statusBarSize)
        fragmentListsIcons.updateTopMargin(dimenToPx(R.dimen.listsIconsPadding) + statusBarSize)
        fragmentListsSearchLocalView.updateTopMargin(dimenToPx(R.dimen.listsSearchLocalViewPadding) + statusBarSize)
        fragmentListsEmptyView.root.updateTopMargin(statusBarSize)
        fragmentListsSnackHost.updateLayoutParams<MarginLayoutParams> {
          updateMargins(bottom = inset.bottom + dimenToPx(R.dimen.listsFabBottomPadding))
        }
      }
    }
  }

  private fun setupRecycler() {
    layoutManager = ListsLayoutManagerProvider.provideLayoutManger(requireContext(), settings)
    adapter =
      ListsAdapter().apply {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
        itemClickListener = { openListDetails(it) }
        itemsChangedListener = {
          resetTranslations()
          layoutManager?.scrollToPosition(0)
        }
        missingImageListener = { item, itemImage, force ->
          viewModel.loadMissingImage(item, itemImage, force)
        }
      }
    with(binding) {
      fragmentListsRecycler.apply {
        adapter = this@ListsFragment.adapter
        layoutManager = this@ListsFragment.layoutManager
        (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        setHasFixedSize(true)
        clearOnScrollListeners()
        addOnScrollListener(
          object : RecyclerView.OnScrollListener() {
            var isFading = false

            override fun onScrolled(
              recyclerView: RecyclerView,
              dx: Int,
              dy: Int,
            ) {
              if (isFading) return
              val position =
                (this@ListsFragment.layoutManager as? LinearLayoutManager)
                  ?.findFirstCompletelyVisibleItemPosition()
                  ?: (this@ListsFragment.layoutManager as? GridLayoutManager)?.findFirstCompletelyVisibleItemPosition()
              if ((position ?: 0) > 1) {
                if (fragmentListsCreateListButton.visibility != VISIBLE) return
                fragmentListsCreateListButton
                  .fadeOut(125, endAction = { isFading = false })
                  .add(animations)
              } else {
                if (fragmentListsCreateListButton.visibility != GONE) return
                fragmentListsCreateListButton
                  .fadeIn(125, endAction = { isFading = false })
                  .add(animations)
              }
              isFading = true
            }
          },
        )
      }
    }
  }

  override fun setupBackPressed() {
    val dispatcher = requireActivity().onBackPressedDispatcher
    dispatcher.addCallback(viewLifecycleOwner) {
      if (isSearching) {
        exitSearch()
      } else {
        isEnabled = false
        dispatcher.onBackPressed()
      }
    }
  }

  private fun enterSearch() {
    resetTranslations()
    with(binding) {
      fragmentListsSearchLocalView.fadeIn(150)
      fragmentListsIcons.gone()
      fragmentListsRecycler.smoothScrollToPosition(0)
      with(fragmentListsSearchLocalView.binding.searchViewLocalInput) {
        setText("")
        doAfterTextChanged {
          viewModel.loadItems(
            searchQuery = it.toString().trim(),
            resetScroll = true,
          )
        }
        visible()
        showKeyboard()
        requestFocus()
      }
    }
    isSearching = true
  }

  private fun exitSearch() {
    with(binding) {
      isSearching = false
      resetTranslations()
      fragmentListsSearchLocalView.gone()
      fragmentListsIcons.visible()
      fragmentListsRecycler.translationY = 0F
      fragmentListsRecycler.postDelayed(200) { layoutManager?.scrollToPosition(0) }
      with(fragmentListsSearchLocalView.binding.searchViewLocalInput) {
        setText("")
        gone()
        hideKeyboard()
        clearFocus()
      }
    }
  }

  private fun showSortOrderDialog(
    sortOrder: SortOrder,
    sortType: SortType,
  ) {
    val options = listOf(NAME, NEWEST, DATE_UPDATED)
    val args = SortOrderBottomSheet.createBundle(options, sortOrder, sortType)

    setFragmentResultListener(NavigationArgs.REQUEST_SORT_ORDER) { _, bundle ->
      val order = bundle.requireSerializable<SortOrder>(NavigationArgs.ARG_SELECTED_SORT_ORDER)
      val type = bundle.requireSerializable<SortType>(NavigationArgs.ARG_SELECTED_SORT_TYPE)
      viewModel.setSortOrder(order, type)
    }

    navigateToSafe(R.id.actionListsFragmentToSortOrder, args)
  }

  private fun render(uiState: ListsUiState) {
    uiState.run {
      with(binding) {
        items?.let {
          fragmentListsEmptyView.root.fadeIf(it.isEmpty() && !isSearching)
          fragmentListsSearchButton.visibleIf(it.isNotEmpty() || isSearching)

          val resetScroll = resetScroll.consume() == true
          adapter?.setItems(it, resetScroll)
        }
        sortOrder?.let {
          fragmentListsFilters.setSorting(it.first, it.second)
        }
      }
    }
  }

  private fun openMainSearch() {
    disableUi()
    hideNavigation()
    with(binding) {
      fragmentListsModeTabs.fadeOut(duration = 200).add(animations)
      fragmentListsIcons.fadeOut(duration = 200).add(animations)
      fragmentListsRecycler
        .fadeOut(duration = 200) {
          super.navigateTo(R.id.actionListsFragmentToSearch, null)
        }.add(animations)
    }
  }

  private fun openListDetails(listItem: ListsItem) {
    disableUi()
    hideNavigation()
    binding.fragmentListsRoot
      .fadeOut(150) {
        val bundle = Bundle().apply { putParcelable(ARG_LIST, listItem.list) }
        navigateToSafe(R.id.actionListsFragmentToDetailsFragment, bundle)
        exitSearch()
      }.add(animations)
  }

  private fun openSettings() {
    hideNavigation()
    exitSearch()
    navigateToSafe(R.id.actionListsFragmentToSettingsFragment)
  }

  private fun openCreateList() {
    setFragmentResultListener(REQUEST_CREATE_LIST) { _, _ -> viewModel.loadItems(resetScroll = true) }
    navigateToSafe(R.id.actionListsFragmentToCreateListDialog, Bundle())
  }

  private fun resetTranslations(duration: Long = TRANSLATION_DURATION) {
    if (view == null) return
    with(binding) {
      arrayOf(
        fragmentListsSearchView,
        fragmentListsModeTabs,
        fragmentListsIcons,
        fragmentListsSearchLocalView,
      ).forEach {
        it
          .animate()
          .translationY(0F)
          .setDuration(duration)
          .add(animations)
          ?.start()
      }
    }
  }

  private fun handleEvent(event: Event) = Unit

  override fun onTabReselected() {
    if (view == null) return
    resetTranslations()
    binding.fragmentListsRecycler.smoothScrollToPosition(0)
  }

  override fun onDestroyView() {
    adapter = null
    layoutManager = null
    super.onDestroyView()
  }
}
