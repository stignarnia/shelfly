package xyz.stignarnia.ui_progress_movies.main

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
import androidx.viewpager.widget.ViewPager
import xyz.stignarnia.ui_base.BaseFragment
import xyz.stignarnia.ui_base.common.OnScrollResetListener
import xyz.stignarnia.ui_base.common.OnSearchClickListener
import xyz.stignarnia.ui_base.common.OnShowsMoviesSyncedListener
import xyz.stignarnia.ui_base.common.OnTabReselectedListener
import xyz.stignarnia.ui_base.common.sheets.context_menu.ContextMenuBottomSheet
import xyz.stignarnia.ui_base.common.sheets.date_selection.DateSelectionBottomSheet
import xyz.stignarnia.ui_base.common.sheets.date_selection.DateSelectionBottomSheet.Companion.REQUEST_DATE_SELECTION
import xyz.stignarnia.ui_base.common.sheets.date_selection.DateSelectionBottomSheet.Result
import xyz.stignarnia.ui_base.utilities.extensions.add
import xyz.stignarnia.ui_base.utilities.extensions.dimenToPx
import xyz.stignarnia.ui_base.utilities.extensions.disableUi
import xyz.stignarnia.ui_base.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.ui_base.utilities.extensions.enableUi
import xyz.stignarnia.ui_base.utilities.extensions.fadeIn
import xyz.stignarnia.ui_base.utilities.extensions.fadeOut
import xyz.stignarnia.ui_base.utilities.extensions.gone
import xyz.stignarnia.ui_base.utilities.extensions.hideKeyboard
import xyz.stignarnia.ui_base.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.ui_base.utilities.extensions.navigateToSafe
import xyz.stignarnia.ui_base.utilities.extensions.nextPage
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.requireParcelable
import xyz.stignarnia.ui_base.utilities.extensions.showKeyboard
import xyz.stignarnia.ui_base.utilities.extensions.visible
import xyz.stignarnia.ui_base.utilities.extensions.visibleIf
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_navigation.java.NavigationArgs
import xyz.stignarnia.ui_navigation.java.NavigationArgs.ARG_MOVIE_ID
import xyz.stignarnia.ui_progress_movies.R
import xyz.stignarnia.ui_progress_movies.databinding.FragmentProgressMainMoviesBinding
import dagger.hilt.android.AndroidEntryPoint
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime

@AndroidEntryPoint
class ProgressMoviesMainFragment :
  BaseFragment<ProgressMoviesMainViewModel>(R.layout.fragment_progress_main_movies),
  OnShowsMoviesSyncedListener,
  OnTabReselectedListener {

  companion object {
    private const val TRANSLATION_DURATION = 225L
  }

  override val navigationId = R.id.progressMoviesMainFragment

  override val viewModel by viewModels<ProgressMoviesMainViewModel>()
  private val binding by viewBinding(FragmentProgressMainMoviesBinding::bind)

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
      searchViewTranslation = progressMoviesSearchView.translationY
      tabsTranslation = progressMoviesTabs.translationY
      sideIconTranslation = progressMoviesSideIcons.translationY
    }
    super.onPause()
  }

  private fun setupView() {
    with(binding) {
      with(progressMoviesSearchIcon) {
        onClick { if (!isSearching) enterSearch() else exitSearch() }
      }

      with(progressMoviesSearchView) {
        hint = getString(R.string.textSearchFor)
        settingsIconVisible = true
        isClickable = false
        onClick { openMainSearch() }
        onSettingsClickListener = { openSettings() }
      }

      with(progressMoviesModeTabs) {
        visibleIf(moviesEnabled)
        onModeSelected = { mode = it }
        selectMovies()
      }

      with(progressMoviesSearchLocalView) {
        onCloseClickListener = { exitSearch() }
      }

      progressMoviesTabs.translationY = tabsTranslation
      progressMoviesModeTabs.translationY = tabsTranslation
      progressMoviesSearchView.translationY = searchViewTranslation
      progressMoviesSideIcons.translationY = sideIconTranslation
    }
  }

  private fun setupPager() {
    with(binding) {
      progressMoviesPager.run {
        offscreenPageLimit = ProgressMoviesMainAdapter.PAGES_COUNT
        adapter = ProgressMoviesMainAdapter(childFragmentManager, requireContext())
        addOnPageChangeListener(pageChangeListener)
      }
      progressMoviesTabs.setupWithViewPager(progressMoviesPager)
    }
  }

  private fun setupInsets() {
    with(binding) {
      progressMoviesRoot.doOnApplyWindowInsets { _, insets, _, _ ->
        val tabletOffset = if (isTablet) dimenToPx(R.dimen.spaceMedium) else 0
        val statusBarSize = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top + tabletOffset
        (progressMoviesSearchView.layoutParams as ViewGroup.MarginLayoutParams)
          .updateMargins(top = statusBarSize + dimenToPx(R.dimen.spaceMedium))
        (progressMoviesModeTabs.layoutParams as ViewGroup.MarginLayoutParams)
          .updateMargins(top = statusBarSize + dimenToPx(R.dimen.collectionTabsMargin))
        (progressMoviesSearchLocalView.layoutParams as ViewGroup.MarginLayoutParams)
          .updateMargins(top = statusBarSize + dimenToPx(R.dimen.progressMoviesSearchLocalViewPadding))
        arrayOf(progressMoviesSideIcons, progressMoviesTabs).forEach {
          val margin = statusBarSize + dimenToPx(R.dimen.progressMoviesSearchViewPadding)
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

  fun openMovieDetails(movie: Movie) {
    hideNavigation()
    binding.progressMoviesRoot
      .fadeOut(150) {
        val bundle = Bundle().apply { putLong(ARG_MOVIE_ID, movie.ids.tmdb.id) }
        navigateTo(R.id.actionProgressMoviesFragmentToMovieDetailsFragment, bundle)
        exitSearch()
      }.add(animations)
  }

  fun openMovieMenu(
    movie: Movie,
    showPinButtons: Boolean = true,
  ) {
    setFragmentResultListener(NavigationArgs.REQUEST_ITEM_MENU) { requestKey, _ ->
      if (requestKey == NavigationArgs.REQUEST_ITEM_MENU) {
        viewModel.loadProgress()
      }
      clearFragmentResultListener(NavigationArgs.REQUEST_ITEM_MENU)
    }
    val bundle = ContextMenuBottomSheet.createBundle(movie.ids.tmdb, showPinButtons)
    navigateToSafe(R.id.actionProgressMoviesFragmentToItemMenu, bundle)
  }

  fun openDateSelectionDialog(movie: Movie) {
    fun openRateDialogIfNeeded(customDate: ZonedDateTime? = null) {
      viewModel.setWatchedMovie(movie, customDate)
    }

    setFragmentResultListener(REQUEST_DATE_SELECTION) { _, bundle ->
      when (
        val result = bundle.requireParcelable<Result>(
          DateSelectionBottomSheet.RESULT_DATE_SELECTION,
        )
      ) {
        is Result.Now -> openRateDialogIfNeeded()
        is Result.CustomDate -> openRateDialogIfNeeded(result.date)
        is Result.ReleaseDate -> openRateDialogIfNeeded(result.date)
      }
    }
    val options = DateSelectionBottomSheet.createBundle(movie.released?.atStartOfDay(UTC))
    navigateToSafe(R.id.actionProgressMoviesFragmentToDateSelection, options)
  }

  private fun openSettings() {
    hideNavigation()
    exitSearch()
    navigateToSafe(R.id.actionProgressMoviesFragmentToSettingsFragment)
  }

  private fun openMainSearch() {
    disableUi()
    hideNavigation()
    with(binding) {
      progressMoviesModeTabs.fadeOut(duration = 200).add(animations)
      progressMoviesTabs.fadeOut(duration = 200).add(animations)
      progressMoviesSideIcons.fadeOut(duration = 200).add(animations)
      progressMoviesPager
        .fadeOut(duration = 200) {
          navigateToSafe(R.id.actionProgressMoviesFragmentToSearch)
        }.add(animations)
    }
  }

  private fun enterSearch() {
    binding.progressMoviesSearchLocalView.fadeIn(150)
    resetTranslations()
    with(binding.progressMoviesSearchLocalView.binding.searchViewLocalInput) {
      setText("")
      doAfterTextChanged { viewModel.onSearchQuery(it?.toString() ?: "") }
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
    binding.progressMoviesSearchLocalView.gone()
    resetTranslations()
    with(binding.progressMoviesSearchLocalView.binding.searchViewLocalInput) {
      setText("")
      gone()
      hideKeyboard()
      clearFocus()
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
    binding.progressMoviesPager.nextPage()
    onScrollReset()
  }

  fun resetTranslations(duration: Long = TRANSLATION_DURATION) {
    if (view == null) return
    with(binding) {
      arrayOf(
        progressMoviesSearchView,
        progressMoviesTabs,
        progressMoviesModeTabs,
        progressMoviesSideIcons,
        progressMoviesSearchLocalView,
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

  private fun render(uiState: ProgressMoviesMainUiState) {
    with(binding) {
    }
  }

  private val pageChangeListener = object : ViewPager.OnPageChangeListener {
    override fun onPageSelected(position: Int) {
      if (currentPage == position) return

      if (binding.progressMoviesTabs.translationY != 0F) {
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
