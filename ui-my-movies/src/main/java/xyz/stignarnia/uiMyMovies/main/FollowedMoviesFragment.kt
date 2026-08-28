package xyz.stignarnia.uiMyMovies.main

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
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiMyMovies.R
import xyz.stignarnia.uiMyMovies.databinding.FragmentFollowedMoviesBinding
import xyz.stignarnia.uiNavigation.java.NavigationArgs
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_MOVIE_ID

@AndroidEntryPoint
class FollowedMoviesFragment :
  BaseFragment<FollowedMoviesViewModel>(R.layout.fragment_followed_movies),
  OnTabReselectedListener {
  companion object {
    private const val TRANSLATION_DURATION = 225L
  }

  override val navigationId = R.id.followedMoviesFragment

  override val viewModel by viewModels<FollowedMoviesViewModel>()
  private val binding by viewBinding(FragmentFollowedMoviesBinding::bind)

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
    tabsViewTranslation = binding.followedMoviesTabs.translationY
    searchViewTranslation = binding.followedMoviesSearchView.translationY
    super.onPause()
  }

  override fun onDestroyView() {
    tabsMediator?.detach()
    tabsMediator = null
    binding.followedMoviesPager.unregisterOnPageChangeCallback(pageChangeListener)
    binding.followedMoviesPager.adapter = null
    pagesAdapter = null
    super.onDestroyView()
  }

  private fun setupView() {
    with(binding) {
      followedMoviesSearchView.run {
        hint = getString(R.string.textSearchFor)
        statsIconVisible = true
        onClick { openMainSearch() }
        onSettingsClickListener = { openSettings() }
        onStatsClickListener = { openStatistics() }
      }
      with(followedMoviesSearchLocalView) {
        onCloseClickListener = { exitSearch() }
      }
      followedMoviesModeTabs.run {
        onModeSelected = { mode = it }
        onListsSelected = { navigateTo(R.id.actionNavigateListsFragment) }
        showLists(true)
        selectMovies()
      }
      followedMoviesSearchIcon.run {
        onClick { if (!isSearching) enterSearch() else exitSearch() }
      }

      followedMoviesSearchView.translationY = searchViewTranslation
      followedMoviesTabs.translationY = tabsViewTranslation
      followedMoviesModeTabs.translationY = tabsViewTranslation
      followedMoviesIcons.translationY = tabsViewTranslation
    }
  }

  private fun setupPager() {
    with(binding) {
      pagesAdapter = FollowedPagesAdapter(childFragmentManager, viewLifecycleOwner.lifecycle, requireContext())
      followedMoviesPager.run {
        // No offscreenPageLimit: FragmentStateAdapter saves and restores each page's state, so holding them all live is no longer what keeps a tab intact.
        adapter = pagesAdapter
        registerOnPageChangeCallback(pageChangeListener)
      }
      // ViewPager2 carries no page titles, so the mediator asks the adapter for each one as it binds the tab.
      tabsMediator =
        TabLayoutMediator(followedMoviesTabs, followedMoviesPager) { tab, position ->
          tab.text = pagesAdapter?.getPageTitle(position)
        }.also { it.attach() }
    }
  }

  private fun setupInsets() {
    with(binding) {
      followedMoviesRoot.doOnApplyWindowInsets { _, insets, _, _ ->
        val tabletOffset = if (isTablet) dimenToPx(R.dimen.spaceMedium) else 0
        val statusBarSize = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top + tabletOffset
        followedMoviesSearchView.applyWindowInsetBehaviour(dimenToPx(R.dimen.spaceNormal) + statusBarSize)
        followedMoviesSearchView.updateTopMargin(dimenToPx(R.dimen.spaceMedium) + statusBarSize)
        followedMoviesTabs.updateTopMargin(dimenToPx(R.dimen.myMoviesSearchViewPadding) + statusBarSize)
        followedMoviesModeTabs.updateTopMargin(dimenToPx(R.dimen.collectionTabsMargin) + statusBarSize)
        followedMoviesIcons.updateTopMargin(dimenToPx(R.dimen.myMoviesSearchViewPadding) + statusBarSize)
        followedMoviesSearchLocalView.updateTopMargin(dimenToPx(R.dimen.myMoviesSearchLocalViewPadding) + statusBarSize)
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
    binding.followedMoviesSearchLocalView.fadeIn(150)
    with(binding.followedMoviesSearchLocalView.binding.searchViewLocalInput) {
      setText("")
      doAfterTextChanged { viewModel.onSearchQuery(it?.toString()) }
      visible()
      showKeyboard()
      requestFocus()
    }
    isSearching = true
    childFragmentManager.fragments.forEach { (it as? OnSearchClickListener)?.onEnterSearch() }
  }

  private fun exitSearch() {
    isSearching = false
    childFragmentManager.fragments.forEach { (it as? OnSearchClickListener)?.onExitSearch() }
    resetTranslations()
    binding.followedMoviesSearchLocalView.gone()
    with(binding.followedMoviesSearchLocalView.binding.searchViewLocalInput) {
      setText("")
      gone()
      hideKeyboard()
      clearFocus()
    }
  }

  private fun openMainSearch() {
    disableUi()
    hideNavigation()
    with(binding) {
      followedMoviesModeTabs.fadeOut(duration = 200).add(animations)
      followedMoviesTabs.fadeOut(duration = 200).add(animations)
      followedMoviesIcons.fadeOut(duration = 200).add(animations)
      followedMoviesPager
        .fadeOut(duration = 200) {
          super.navigateTo(R.id.actionFollowedMoviesFragmentToSearch, null)
        }.add(animations)
    }
  }

  fun openMovieDetails(movie: Movie) {
    disableUi()
    hideNavigation()
    binding.followedMoviesRoot
      .fadeOut(150) {
        val bundle = Bundle().apply { putLong(ARG_MOVIE_ID, movie.tmdbId) }
        navigateToSafe(R.id.actionFollowedMoviesFragmentToMovieDetailsFragment, bundle)
        exitSearch()
      }.add(animations)
  }

  fun openMovieMenu(movie: Movie) {
    setFragmentResultListener(NavigationArgs.REQUEST_ITEM_MENU) { requestKey, _ ->
      if (requestKey == NavigationArgs.REQUEST_ITEM_MENU) {
        viewModel.refreshData()
      }
      clearFragmentResultListener(NavigationArgs.REQUEST_ITEM_MENU)
    }
    val bundle = ContextMenuBottomSheet.createBundle(movie.ids.tmdb)
    navigateToSafe(R.id.actionFollowedMoviesFragmentToItemMenu, bundle)
  }

  private fun openSettings() {
    hideNavigation()
    exitSearch()
    navigateToSafe(R.id.actionFollowedMoviesFragmentToSettingsFragment)
  }

  private fun openStatistics() {
    hideNavigation()
    exitSearch()
    navigateToSafe(R.id.actionFollowedMoviesFragmentToStatisticsFragment)
  }

  override fun onTabReselected() {
    if (view == null) return
    resetTranslations(duration = 0)
    binding.followedMoviesPager.nextPage()
    childFragmentManager.fragments.forEach {
      (it as? OnScrollResetListener)?.onScrollReset()
    }
  }

  fun resetTranslations(duration: Long = TRANSLATION_DURATION) {
    if (view == null) return
    with(binding) {
      arrayOf(
        followedMoviesSearchView,
        followedMoviesTabs,
        followedMoviesModeTabs,
        followedMoviesIcons,
        followedMoviesSearchLocalView,
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

        if (binding.followedMoviesTabs.translationY != 0F) {
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
