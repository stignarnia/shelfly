package xyz.stignarnia.uiProgress.main

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateMargins
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
import xyz.stignarnia.uiBase.common.OnShowsMoviesSyncedListener
import xyz.stignarnia.uiBase.common.OnTabReselectedListener
import xyz.stignarnia.uiBase.common.sheets.contextMenu.ContextMenuBottomSheet
import xyz.stignarnia.uiBase.common.sheets.dateSelection.DateSelectionBottomSheet
import xyz.stignarnia.uiBase.common.sheets.dateSelection.DateSelectionBottomSheet.Companion.REQUEST_DATE_SELECTION
import xyz.stignarnia.uiBase.common.sheets.dateSelection.DateSelectionBottomSheet.Companion.RESULT_DATE_SELECTION
import xyz.stignarnia.uiBase.common.sheets.dateSelection.DateSelectionBottomSheet.Result
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiBase.utilities.extensions.add
import xyz.stignarnia.uiBase.utilities.extensions.dimenToPx
import xyz.stignarnia.uiBase.utilities.extensions.disableUi
import xyz.stignarnia.uiBase.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.uiBase.utilities.extensions.enableUi
import xyz.stignarnia.uiBase.utilities.extensions.fadeIn
import xyz.stignarnia.uiBase.utilities.extensions.fadeOut
import xyz.stignarnia.uiBase.utilities.extensions.gone
import xyz.stignarnia.uiBase.utilities.extensions.hideKeyboard
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.navigateToSafe
import xyz.stignarnia.uiBase.utilities.extensions.nextPage
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.requireParcelable
import xyz.stignarnia.uiBase.utilities.extensions.showKeyboard
import xyz.stignarnia.uiBase.utilities.extensions.visible
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiEpisodes.details.EpisodeDetailsBottomSheet
import xyz.stignarnia.uiModel.Episode
import xyz.stignarnia.uiModel.EpisodeBundle
import xyz.stignarnia.uiModel.Season
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ACTION_EPISODE_TAB_SELECTED
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_SHOW_ID
import xyz.stignarnia.uiNavigation.java.NavigationArgs.REQUEST_EPISODE_DETAILS
import xyz.stignarnia.uiNavigation.java.NavigationArgs.REQUEST_ITEM_MENU
import xyz.stignarnia.uiProgress.R
import xyz.stignarnia.uiProgress.databinding.FragmentProgressMainBinding
import xyz.stignarnia.uiProgress.main.adapters.ProgressMainAdapter
import java.time.ZonedDateTime

@AndroidEntryPoint
class ProgressMainFragment :
  BaseFragment<ProgressMainViewModel>(R.layout.fragment_progress_main),
  OnShowsMoviesSyncedListener,
  OnTabReselectedListener {
  companion object {
    private const val TRANSLATION_DURATION = 225L
  }

  override val navigationId = R.id.progressMainFragment

  override val viewModel by viewModels<ProgressMainViewModel>()
  private val binding by viewBinding(FragmentProgressMainBinding::bind)

  private var adapter: ProgressMainAdapter? = null
  private var tabsMediator: TabLayoutMediator? = null

  private var searchViewTranslation = 0F
  private var tabsTranslation = 0F
  private var sideIconTranslation = 0F
  private var currentPage = 0
  private var isSearching = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    savedInstanceState?.let {
      searchViewTranslation = it.getFloat("ARG_SEARCH_POSITION")
      tabsTranslation = it.getFloat("ARG_TABS_POSITION")
      sideIconTranslation = it.getFloat("ARG_SIDE_ICON_POSITION")
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

    launchAndRepeatStarted(
      { viewModel.uiState.collect { render(it) } },
      { viewModel.messageFlow.collect { showSnack(it) } },
      { viewModel.eventFlow.collect { handleEvent(it) } },
      doAfterLaunch = { viewModel.loadProgress() },
    )
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putFloat("ARG_SEARCH_POSITION", searchViewTranslation)
    outState.putFloat("ARG_TABS_POSITION", tabsTranslation)
    outState.putFloat("ARG_SIDE_ICON_POSITION", sideIconTranslation)
    outState.putInt("ARG_PAGE", currentPage)
  }

  override fun onResume() {
    super.onResume()
    showNavigation()
  }

  override fun onPause() {
    enableUi()
    with(binding) {
      tabsTranslation = progressMainTabs.translationY
      searchViewTranslation = progressMainSearchView.translationY
      sideIconTranslation = progressMainSideIcons.translationY
    }
    super.onPause()
  }

  override fun onDestroyView() {
    with(binding) {
      tabsMediator?.detach()
      tabsMediator = null
      progressMainPager.unregisterOnPageChangeCallback(pageChangeListener)
      progressMainPager.adapter = null
    }
    adapter = null
    super.onDestroyView()
  }

  private fun setupView() {
    with(binding) {
      with(progressMainSearchIcon) {
        onClick { if (!isSearching) enterSearch() else exitSearch() }
      }

      with(progressMainSearchView) {
        hint = getString(R.string.textSearchFor)
        settingsIconVisible = true
        isClickable = false
        onClick { openMainSearch() }
        onSettingsClickListener = { openSettings() }
      }

      with(progressMainSearchLocalView) {
        onCloseClickListener = { exitSearch() }
      }

      with(progressMainPagerModeTabs) {
        visibleIf(moviesEnabled)
        onModeSelected = { mode = it }
        selectShows()
      }

      progressMainTabs.translationY = tabsTranslation
      progressMainPagerModeTabs.translationY = tabsTranslation
      progressMainSearchView.translationY = searchViewTranslation
      progressMainSideIcons.translationY = sideIconTranslation
    }
  }

  private fun setupPager() {
    adapter = ProgressMainAdapter(childFragmentManager, viewLifecycleOwner.lifecycle, requireContext())
    with(binding) {
      progressMainPager.run {
        // No offscreenPageLimit: FragmentStateAdapter saves and restores each page's state, so holding them all live is no longer what keeps a tab intact.
        adapter = this@ProgressMainFragment.adapter
        registerOnPageChangeCallback(pageChangeListener)
      }
      // ViewPager2 carries no page titles, so the mediator asks the adapter for each one as it binds the tab.
      tabsMediator =
        TabLayoutMediator(progressMainTabs, progressMainPager) { tab, position ->
          tab.text = adapter?.getPageTitle(position)
        }.also { it.attach() }
    }
  }

  private fun setupInsets() {
    with(binding) {
      progressMainRoot.doOnApplyWindowInsets { _, insets, _, _ ->
        val tabletOffset = if (isTablet) dimenToPx(R.dimen.spaceMedium) else 0
        val statusBarSize = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top + tabletOffset
        val progressTabsMargin =
          if (moviesEnabled) {
            R.dimen.progressSearchViewPadding
          } else {
            R.dimen.progressSearchViewPaddingNoModes
          }

        val progressMainSearchLocalMargin =
          if (moviesEnabled) R.dimen.progressSearchLocalViewPadding else R.dimen.progressSearchLocalViewPaddingNoModes
        (progressMainSearchView.layoutParams as ViewGroup.MarginLayoutParams)
          .updateMargins(top = statusBarSize + dimenToPx(R.dimen.spaceMedium))
        (progressMainSearchLocalView.layoutParams as ViewGroup.MarginLayoutParams)
          .updateMargins(top = statusBarSize + dimenToPx(progressMainSearchLocalMargin))
        (progressMainPagerModeTabs.layoutParams as ViewGroup.MarginLayoutParams)
          .updateMargins(top = statusBarSize + dimenToPx(R.dimen.collectionTabsMargin))
        arrayOf(progressMainTabs, progressMainSideIcons).forEach {
          val margin = statusBarSize + dimenToPx(progressTabsMargin)
          (it.layoutParams as ViewGroup.MarginLayoutParams).updateMargins(top = margin)
        }
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

  private fun openMainSearch() {
    with(binding) {
      disableUi()
      hideNavigation()
      progressMainPagerModeTabs.fadeOut(duration = 200).add(animations)
      progressMainTabs.fadeOut(duration = 200).add(animations)
      progressMainSideIcons.fadeOut(duration = 200).add(animations)
      progressMainPager
        .fadeOut(duration = 200) {
          navigateToSafe(R.id.actionProgressFragmentToSearch)
        }.add(animations)
    }
  }

  fun openShowDetails(show: Show) {
    with(binding) {
      hideNavigation()
      progressMainRoot
        .fadeOut(150) {
          if (findNavControl()?.currentDestination?.id == R.id.progressMainFragment) {
            val bundle = Bundle().apply { putLong(ARG_SHOW_ID, show.tmdbId) }
            navigateToSafe(R.id.actionProgressFragmentToShowDetailsFragment, bundle)
            exitSearch()
          } else {
            showNavigation()
            progressMainRoot.fadeIn(50).add(animations)
          }
        }.add(animations)
    }
  }

  fun openShowMenu(show: Show) {
    setFragmentResultListener(REQUEST_ITEM_MENU) { requestKey, _ ->
      if (requestKey == REQUEST_ITEM_MENU) {
        viewModel.loadProgress()
      }
      clearFragmentResultListener(REQUEST_ITEM_MENU)
    }
    val bundle = ContextMenuBottomSheet.createBundle(show.ids.tmdb, showPinButtons = true)
    navigateToSafe(R.id.actionProgressFragmentToItemMenu, bundle)
  }

  fun openEpisodeDetails(
    show: Show,
    episode: Episode,
    season: Season,
  ) {
    setFragmentResultListener(REQUEST_EPISODE_DETAILS) { _, bundle ->
      when {
        bundle.containsKey(ACTION_EPISODE_TAB_SELECTED) -> {
          val selectedEpisode = bundle.requireParcelable<Episode>(ACTION_EPISODE_TAB_SELECTED)
          openEpisodeDetails(show, selectedEpisode, season)
        }
      }
    }
    viewModel.onEpisodeDetails(show, episode)
  }

  fun openDateSelectionDialog(episodeBundle: EpisodeBundle) {
    fun openRateDialogIfNeeded(customDate: ZonedDateTime? = null) {
      viewModel.setWatchedEpisode(episodeBundle, customDate)
    }

    setFragmentResultListener(REQUEST_DATE_SELECTION) { _, bundle ->
      when (val result = bundle.requireParcelable<Result>(RESULT_DATE_SELECTION)) {
        is Result.Now -> openRateDialogIfNeeded()
        is Result.CustomDate -> openRateDialogIfNeeded(result.date)
        is Result.ReleaseDate -> openRateDialogIfNeeded(result.date)
      }
    }
    val options = DateSelectionBottomSheet.createBundle(episodeBundle.episode.firstAired)
    navigateToSafe(R.id.actionProgressFragmentToDateSelection, options)
  }

  private fun openSettings() {
    hideNavigation()
    exitSearch()
    navigateToSafe(R.id.actionProgressFragmentToSettingsFragment)
  }

  private fun enterSearch() {
    resetTranslations()
    with(binding) {
      progressMainSearchLocalView.fadeIn(150)
      with(progressMainSearchLocalView.binding.searchViewLocalInput) {
        setText("")
        doAfterTextChanged { viewModel.onSearchQuery(it?.toString()) }
        visible()
        showKeyboard()
        requestFocus()
      }
    }
    isSearching = true
    childFragmentManager.fragments.forEach { (it as? OnSearchClickListener)?.onEnterSearch() }
  }

  private fun exitSearch() {
    isSearching = false
    childFragmentManager.fragments.forEach { (it as? OnSearchClickListener)?.onExitSearch() }
    resetTranslations()
    with(binding) {
      progressMainSearchLocalView.gone()
      with(progressMainSearchLocalView.binding.searchViewLocalInput) {
        setText("")
        gone()
        hideKeyboard()
        clearFocus()
      }
    }
  }

  fun toggleCalendarMode() {
    exitSearch()
    onScrollReset()
    resetTranslations()
    viewModel.toggleCalendarMode()
  }

  override fun onShowsMoviesSyncFinished() = viewModel.loadProgress()

  override fun onTabReselected() {
    if (view == null) return
    resetTranslations(duration = 0)
    binding.progressMainPager.nextPage()
    onScrollReset()
  }

  fun resetTranslations(duration: Long = TRANSLATION_DURATION) {
    if (view == null) return
    with(binding) {
      arrayOf(
        progressMainSearchView,
        progressMainTabs,
        progressMainPagerModeTabs,
        progressMainSideIcons,
        progressMainSearchLocalView,
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

  private fun onScrollReset() =
    childFragmentManager.fragments.forEach { (it as? OnScrollResetListener)?.onScrollReset() }

  private fun render(uiState: ProgressMainUiState) {
    with(binding) {
    }
  }

  private fun handleEvent(event: Event<*>) {
    when (event) {
      is OpenEpisodeDetails -> {
        val bundle =
          EpisodeDetailsBottomSheet.createBundle(
            showIds = event.show.ids,
            episode = event.episode,
            seasonEpisodesIds = null,
            isWatched = event.isWatched,
            showTabs = true,
          )
        navigateToSafe(R.id.actionProgressFragmentToEpisodeDetails, bundle)
      }
    }
  }

  private val pageChangeListener =
    object : ViewPager2.OnPageChangeCallback() {
      override fun onPageSelected(position: Int) {
        if (currentPage == position) return

        if (binding.progressMainTabs.translationY != 0F) {
          resetTranslations()
          requireView().postDelayed({ onScrollReset() }, TRANSLATION_DURATION)
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
