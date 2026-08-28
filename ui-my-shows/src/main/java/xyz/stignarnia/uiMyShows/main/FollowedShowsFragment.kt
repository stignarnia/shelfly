package xyz.stignarnia.uiMyShows.main

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.common.OnScrollResetListener
import xyz.stignarnia.uiBase.common.OnSearchClickListener
import xyz.stignarnia.uiBase.common.OnTabReselectedListener
import xyz.stignarnia.uiBase.common.sheets.contextMenu.ContextMenuBottomSheet
import xyz.stignarnia.uiBase.utilities.extensions.add
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.disableUi
import xyz.stignarnia.uiBase.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.uiBase.utilities.extensions.enableUi
import xyz.stignarnia.uiBase.utilities.extensions.fadeIn
import xyz.stignarnia.uiBase.utilities.extensions.fadeOut
import xyz.stignarnia.uiBase.utilities.extensions.gone
import xyz.stignarnia.uiBase.utilities.extensions.hideKeyboard
import xyz.stignarnia.uiBase.utilities.extensions.navigateToSafe
import xyz.stignarnia.uiBase.utilities.extensions.nextPage
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.showKeyboard
import xyz.stignarnia.uiBase.utilities.extensions.updateTopMargin
import xyz.stignarnia.uiBase.utilities.extensions.visible
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiMyShows.R
import xyz.stignarnia.uiMyShows.databinding.FragmentFollowedShowsBinding
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_SHOW_ID
import xyz.stignarnia.uiNavigation.java.NavigationArgs.REQUEST_ITEM_MENU

@AndroidEntryPoint
class FollowedShowsFragment :
  BaseFragment<FollowedShowsViewModel>(R.layout.fragment_followed_shows),
  OnTabReselectedListener {
  companion object {
    const val REQUEST_MY_SHOWS_FILTERS = "REQUEST_MY_SHOWS_FILTERS"
    private const val TRANSLATION_DURATION = 225L
  }

  override val navigationId = R.id.followedShowsFragment

  override val viewModel by viewModels<FollowedShowsViewModel>()
  private val binding by viewBinding(FragmentFollowedShowsBinding::bind)

  private var searchViewTranslation = 0F
  private var tabsViewTranslation = 0F
  private var currentPage = 0
  private var pagesAdapter: FollowedPagesAdapter? = null
  private var tabsMediator: TabLayoutMediator? = null
  private var isSearching = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    savedInstanceState?.let {
      searchViewTranslation = it.getFloat("ARG_SEARCH_POSITION")
      tabsViewTranslation = it.getFloat("ARG_TABS_POSITION")
      currentPage = it.getInt("ARG_PAGE")
    }
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    setupPager()
    setupInsets()

    setFragmentResultListener(REQUEST_MY_SHOWS_FILTERS) { _, _ ->
      viewModel.refreshData()
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putFloat("ARG_SEARCH_POSITION", searchViewTranslation)
    outState.putFloat("ARG_TABS_POSITION", tabsViewTranslation)
    outState.putInt("ARG_PAGE", currentPage)
  }

  override fun onResume() {
    super.onResume()
    showNavigation()
  }

  override fun onPause() {
    enableUi()
    tabsViewTranslation = binding.followedShowsTabs.translationY
    searchViewTranslation = binding.followedShowsSearchView.translationY
    super.onPause()
  }

  override fun onDestroyView() {
    tabsMediator?.detach()
    tabsMediator = null
    binding.followedShowsPager.unregisterOnPageChangeCallback(pageChangeListener)
    binding.followedShowsPager.adapter = null
    pagesAdapter = null
    super.onDestroyView()
  }

  private fun setupView() {
    with(binding) {
      followedShowsSearchView.run {
        hint = getString(R.string.textSearchFor)
        statsIconVisible = true
        onClick { openMainSearch() }
        onSettingsClickListener = { openSettings() }
        onStatsClickListener = { openStatistics() }
      }
      with(followedShowsSearchLocalView) {
        onCloseClickListener = { exitSearch() }
      }
      followedShowsModeTabs.run {
        onModeSelected = { mode = it }
        onListsSelected = { navigateTo(R.id.actionNavigateListsFragment) }
        showMovies(moviesEnabled)
        showLists(true, anchorEnd = moviesEnabled)
        selectShows()
      }
      followedShowsSearchIcon.onClick {
        if (!isSearching) enterSearch() else exitSearch()
      }
      followedShowsSearchView.translationY = searchViewTranslation
      followedShowsTabs.translationY = tabsViewTranslation
      followedShowsModeTabs.translationY = tabsViewTranslation
      followedShowsIcons.translationY = tabsViewTranslation
    }
  }

  private fun setupPager() {
    with(binding) {
      pagesAdapter = FollowedPagesAdapter(childFragmentManager, viewLifecycleOwner.lifecycle, requireContext())
      followedShowsPager.run {
        // No offscreenPageLimit: FragmentStateAdapter saves and restores each page's state, so holding them all live is no longer what keeps a tab intact.
        adapter = pagesAdapter
        registerOnPageChangeCallback(pageChangeListener)
      }
      // ViewPager2 carries no page titles, so the mediator asks the adapter for each one as it binds the tab.
      tabsMediator =
        TabLayoutMediator(followedShowsTabs, followedShowsPager) { tab, position ->
          tab.text = pagesAdapter?.getPageTitle(position)
        }.also { it.attach() }
    }
  }

  private fun setupInsets() {
    with(binding) {
      followedShowsRoot.doOnApplyWindowInsets { _, insets, _, _ ->
        val tabletOffset = if (isTablet) dimenToPx(R.dimen.spaceMedium) else 0
        val statusBarSize = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top + tabletOffset
        followedShowsSearchView.applyWindowInsetBehaviour(dimenToPx(R.dimen.spaceNormal) + statusBarSize)
        followedShowsSearchView.updateTopMargin(dimenToPx(R.dimen.spaceMedium) + statusBarSize)
        followedShowsModeTabs.updateTopMargin(dimenToPx(R.dimen.collectionTabsMargin) + statusBarSize)
        followedShowsTabs.updateTopMargin(dimenToPx(R.dimen.myShowsSearchViewPadding) + statusBarSize)
        followedShowsIcons.updateTopMargin(dimenToPx(R.dimen.myShowsSearchViewPadding) + statusBarSize)
        followedShowsSearchLocalView.updateTopMargin(dimenToPx(R.dimen.myShowsSearchLocalViewPadding) + statusBarSize)
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
    with(binding) {
      resetTranslations()
      followedShowsSearchLocalView.fadeIn(150)
      with(followedShowsSearchLocalView.binding.searchViewLocalInput) {
        setText("")
        doAfterTextChanged { viewModel.onSearchQuery(it?.toString()) }
        visible()
        showKeyboard()
        requestFocus()
      }
      isSearching = true
      childFragmentManager.fragments.forEach { (it as? OnSearchClickListener)?.onEnterSearch() }
    }
  }

  private fun exitSearch() {
    with(binding) {
      isSearching = false
      childFragmentManager.fragments.forEach { (it as? OnSearchClickListener)?.onExitSearch() }
      resetTranslations()
      followedShowsSearchLocalView.gone()
      with(followedShowsSearchLocalView.binding.searchViewLocalInput) {
        setText("")
        gone()
        hideKeyboard()
        clearFocus()
      }
    }
  }

  private fun openMainSearch() {
    with(binding) {
      disableUi()
      hideNavigation()
      followedShowsModeTabs.fadeOut(duration = 200).add(animations)
      followedShowsTabs.fadeOut(duration = 200).add(animations)
      followedShowsIcons.fadeOut(duration = 200).add(animations)
      followedShowsPager
        .fadeOut(duration = 200) {
          super.navigateTo(R.id.actionFollowedShowsFragmentToSearch, null)
        }.add(animations)
    }
  }

  fun openShowDetails(show: Show) {
    disableUi()
    hideNavigation()
    binding.followedShowsRoot
      .fadeOut(150) {
        val bundle = Bundle().apply { putLong(ARG_SHOW_ID, show.tmdbId) }
        navigateToSafe(R.id.actionFollowedShowsFragmentToShowDetailsFragment, bundle)
        exitSearch()
      }.add(animations)
  }

  fun openShowMenu(show: Show) {
    setFragmentResultListener(REQUEST_ITEM_MENU) { requestKey, _ ->
      if (requestKey == REQUEST_ITEM_MENU) {
        viewModel.refreshData()
      }
      clearFragmentResultListener(REQUEST_ITEM_MENU)
    }
    val bundle = ContextMenuBottomSheet.createBundle(show.ids.tmdb)
    navigateToSafe(R.id.actionFollowedShowsFragmentToItemMenu, bundle)
  }

  private fun openSettings() {
    hideNavigation()
    exitSearch()
    navigateToSafe(R.id.actionFollowedShowsFragmentToSettingsFragment)
  }

  private fun openStatistics() {
    hideNavigation()
    exitSearch()
    navigateToSafe(R.id.actionFollowedShowsFragmentToStatisticsFragment)
  }

  override fun onTabReselected() {
    if (view == null) return
    resetTranslations(duration = 0)
    binding.followedShowsPager.nextPage()
    childFragmentManager.fragments.forEach {
      (it as? OnScrollResetListener)?.onScrollReset()
    }
  }

  fun resetTranslations(duration: Long = TRANSLATION_DURATION) {
    if (view == null) return
    with(binding) {
      arrayOf(
        followedShowsSearchView,
        followedShowsTabs,
        followedShowsModeTabs,
        followedShowsIcons,
        followedShowsSearchLocalView,
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

  private val pageChangeListener =
    object : ViewPager2.OnPageChangeCallback() {
      override fun onPageSelected(position: Int) {
        if (currentPage == position) return

        if (binding.followedShowsTabs.translationY != 0F) {
          resetTranslations()
          requireView().postDelayed(
            {
              childFragmentManager.fragments.forEach { (it as? OnScrollResetListener)?.onScrollReset() }
            },
            225L,
          )
        }

        currentPage = position
      }

      override fun onPageScrolled(
        position: Int,
        positionOffset: Float,
        positionOffsetPixels: Int,
      ) = Unit

      override fun onPageScrollStateChanged(state: Int) = Unit
    }
}
