package xyz.stignarnia.ui_discover_movies

import android.os.Bundle
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import xyz.stignarnia.ui_base.BaseFragment
import xyz.stignarnia.ui_base.common.OnTabReselectedListener
import xyz.stignarnia.ui_base.common.sheets.context_menu.ContextMenuBottomSheet
import xyz.stignarnia.ui_base.utilities.extensions.add
import xyz.stignarnia.ui_base.utilities.extensions.dimenToPx
import xyz.stignarnia.ui_base.utilities.extensions.disableUi
import xyz.stignarnia.ui_base.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.ui_base.utilities.extensions.enableUi
import xyz.stignarnia.ui_base.utilities.extensions.fadeIn
import xyz.stignarnia.ui_base.utilities.extensions.fadeOut
import xyz.stignarnia.ui_base.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.ui_base.utilities.extensions.navigateToSafe
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.visible
import xyz.stignarnia.ui_base.utilities.extensions.withSpanSizeLookup
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_discover_movies.databinding.FragmentDiscoverMoviesBinding
import xyz.stignarnia.ui_discover_movies.helpers.DiscoverMoviesLayoutManagerProvider
import xyz.stignarnia.ui_discover_movies.recycler.DiscoverMovieListItem
import xyz.stignarnia.ui_discover_movies.recycler.DiscoverMoviesAdapter
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_navigation.java.NavigationArgs
import dagger.hilt.android.AndroidEntryPoint
import kotlin.random.Random

@AndroidEntryPoint
internal class DiscoverMoviesFragment :
  BaseFragment<DiscoverMoviesViewModel>(R.layout.fragment_discover_movies),
  OnTabReselectedListener {

  companion object {
    const val REQUEST_DISCOVER_FILTERS = "REQUEST_DISCOVER_FILTERS"
  }

  private val binding by viewBinding(FragmentDiscoverMoviesBinding::bind)

  override val viewModel by viewModels<DiscoverMoviesViewModel>()
  override val navigationId = R.id.discoverMoviesFragment

  private var adapter: DiscoverMoviesAdapter? = null
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

  override fun onResume() {
    super.onResume()
    showNavigation()
  }

  override fun onPause() {
    enableUi()
    with(binding) {
      searchViewPosition = discoverMoviesSearchView.translationY
      tabsViewPosition = discoverMoviesTabsView.translationY
      filtersViewPosition = discoverMoviesFiltersView.translationY
    }
    super.onPause()
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    setupInsets()
    setupRecycler()
    setupOverscroll()

    launchAndRepeatStarted(
      { viewModel.uiState.collect { render(it) } },
      { viewModel.messageFlow.collect { showSnack(it) } },
      doAfterLaunch = { viewModel.loadMovies() },
    )

    setFragmentResultListener(REQUEST_DISCOVER_FILTERS) { _, _ ->
      viewModel.loadMovies(resetScroll = true, skipCache = true, instantProgress = true)
    }
  }

  private fun setupView() {
    with(binding) {
      discoverMoviesSearchView.run {
        translationY = searchViewPosition
        settingsIconVisible = true
        isEnabled = false
        onClick { openSearch() }
        onSettingsClickListener = {
          hideNavigation()
          navigateToSafe(R.id.actionDiscoverMoviesFragmentToSettingsFragment)
        }
      }
      discoverMoviesTabsView.run {
        translationY = tabsViewPosition
        onModeSelected = { mode = it }
        selectMovies()
      }
      discoverMoviesFiltersView.run {
        translationY = filtersViewPosition
        onGenresChipClick = { navigateToSafe(R.id.actionDiscoverMoviesFragmentToFiltersGenres) }
        onProvidersChipClick = { navigateToSafe(R.id.actionDiscoverMoviesFragmentToFiltersProviders) }
        onFeedChipClick = { navigateToSafe(R.id.actionDiscoverMoviesFragmentToFiltersFeed) }
        onHideCollectionChipClick = { viewModel.toggleCollection() }
      }
    }
  }

  private fun setupInsets() {
    with(binding) {
      discoverMoviesRoot.doOnApplyWindowInsets { _, insets, _, _ ->
        val tabletOffset = if (isTablet) dimenToPx(R.dimen.spaceMedium) else 0
        val statusBarSize = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top + tabletOffset
        // The slot opens past the filter chips, taking the space out of the list's top padding so the content barely shifts.
        // Derived from the same dimens that place the chips, so the ring keeps its gap under them on any screen size instead of trusting one number.
        discoverMoviesOverscroll.openHeight = statusBarSize +
          dimenToPx(R.dimen.collectionFiltersMargin) +
          dimenToPx(R.dimen.chipHeight) +
          dimenToPx(R.dimen.discoverOverscrollGap) +
          dimenToPx(R.dimen.overscrollActionProgress) +
          dimenToPx(R.dimen.spaceMedium)
        val listTopGap = statusBarSize + dimenToPx(R.dimen.discoverRecyclerPadding)
        discoverMoviesOverscroll.restHeight = listTopGap
        // The list spans the whole window and carries the gap under the floating header as its own top padding, so a poster scrolled past the gap slides under the header and off the top of the screen.
        // Ending the list below the slot instead dropped each poster whole the moment it left the list's bounds, which is a row's height short of the screen edge.
        // The negative margin cancels the slot's resting height, so the slot still moves the list by exactly however far it opens past rest and nothing else.
        (discoverMoviesRecycler.layoutParams as ViewGroup.MarginLayoutParams)
          .updateMargins(top = -listTopGap)
        discoverMoviesRecycler
          .updatePadding(top = listTopGap)
        (discoverMoviesSearchView.layoutParams as ViewGroup.MarginLayoutParams)
          .updateMargins(top = statusBarSize + dimenToPx(R.dimen.spaceMedium))
        (discoverMoviesTabsView.layoutParams as ViewGroup.MarginLayoutParams)
          .updateMargins(top = statusBarSize + dimenToPx(R.dimen.collectionTabsMargin))
        (discoverMoviesFiltersView.layoutParams as ViewGroup.MarginLayoutParams)
          .updateMargins(top = statusBarSize + dimenToPx(R.dimen.collectionFiltersMargin))
      }
    }
  }

  private fun setupRecycler() {
    layoutManager = DiscoverMoviesLayoutManagerProvider.provideLayoutManager(requireContext())
    adapter = DiscoverMoviesAdapter(
      itemClickListener = { openDetails(it) },
      itemLongClickListener = { openMovieMenu(it.movie) },
      missingImageListener = { ids, force -> viewModel.loadMissingImage(ids, force) },
      listChangeListener = { binding.discoverMoviesRecycler.scrollToPosition(0) },
    )
    binding.discoverMoviesRecycler.apply {
      adapter = this@DiscoverMoviesFragment.adapter
      layoutManager = this@DiscoverMoviesFragment.layoutManager
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
      setHasFixedSize(true)
    }
  }

  private fun setupOverscroll() {
    with(binding.discoverMoviesOverscroll) {
      onTriggered = {
        searchViewPosition = 0F
        tabsViewPosition = 0F
        viewModel.loadMovies(pullToRefresh = true)
      }
      attach(binding.discoverMoviesRecycler, viewLifecycleOwner)
      // The chips are what the ring is placed under, so they are what it has to leave with when the header scrolls away.
      follow(binding.discoverMoviesFiltersView)
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
      discoverMoviesTabsView.fadeOut(duration = 200).add(animations)
      discoverMoviesFiltersView.fadeOut(duration = 200).add(animations)
      discoverMoviesRecycler
        .fadeOut(duration = 200) {
          navigateToSafe(R.id.actionDiscoverMoviesFragmentToSearchFragment)
        }.add(animations)
    }
  }

  private fun openDetails(item: DiscoverMovieListItem) {
    if (!binding.discoverMoviesRecycler.isEnabled) return
    disableUi()
    hideNavigation()
    animateItemsExit(item)
  }

  private fun openMovieMenu(movie: Movie) {
    if (!binding.discoverMoviesRecycler.isEnabled) return
    setFragmentResultListener(NavigationArgs.REQUEST_ITEM_MENU) { requestKey, _ ->
      if (requestKey == NavigationArgs.REQUEST_ITEM_MENU) {
        viewModel.loadMovies()
      }
      clearFragmentResultListener(NavigationArgs.REQUEST_ITEM_MENU)
    }
    val bundle = ContextMenuBottomSheet.createBundle(movie.ids.tmdb)
    navigateToSafe(R.id.actionDiscoverMoviesFragmentToItemMenu, bundle)
  }

  private fun animateItemsExit(item: DiscoverMovieListItem) {
    with(binding) {
      discoverMoviesSearchView.fadeOut().add(animations)
      discoverMoviesTabsView.fadeOut().add(animations)
      discoverMoviesFiltersView.fadeOut().add(animations)

      val clickedIndex = adapter?.indexOf(item) ?: 0
      val itemCount = adapter?.itemCount ?: 0
      (0..itemCount).forEach {
        if (it != clickedIndex) {
          val view = discoverMoviesRecycler.findViewHolderForAdapterPosition(it)
          view?.let { v ->
            val randomDelay = Random.nextLong(50, 200)
            v.itemView.fadeOut(duration = 150, startDelay = randomDelay).add(animations)
          }
        }
      }

      val clickedView = discoverMoviesRecycler.findViewHolderForAdapterPosition(clickedIndex)
      clickedView
        ?.itemView
        ?.fadeOut(
          duration = 150,
          startDelay = 350,
          endAction = {
            if (!isResumed) return@fadeOut
            val bundle = Bundle().apply { putLong(NavigationArgs.ARG_MOVIE_ID, item.movie.tmdbId) }
            navigateToSafe(R.id.actionDiscoverMoviesFragmentToMovieDetailsFragment, bundle)
          },
        ).add(animations)
    }
  }

  private fun render(uiState: DiscoverMoviesUiState) {
    uiState.run {
      with(binding) {
        items?.let {
          val resetScroll = resetScroll?.consume() == true
          adapter?.setItems(it, resetScroll)
          layoutManager?.withSpanSizeLookup { pos ->
            adapter
              ?.getItems()
              ?.get(pos)
              ?.image
              ?.type
              ?.getSpan(isTablet)!!
          }
          discoverMoviesRecycler.fadeIn(200, withHardware = true)
        }
        isLoading?.let {
          discoverMoviesOverscroll.setRunning(it)
          discoverMoviesSearchView.isEnabled = !it
          discoverMoviesTabsView.isEnabled = !it
          discoverMoviesFiltersView.isEnabled = !it
          discoverMoviesRecycler.isEnabled = !it
        }
        filters?.let {
          if (discoverMoviesFiltersView.visibility != VISIBLE) {
            discoverMoviesFiltersView.visible()
          }
          discoverMoviesFiltersView.bind(it)
        }
      }
    }
  }

  override fun onTabReselected() = openSearch()

  override fun onDestroyView() {
    binding.discoverMoviesOverscroll.detach()
    adapter = null
    layoutManager = null
    super.onDestroyView()
  }
}
