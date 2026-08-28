package xyz.stignarnia.uiMyMovies.hidden

import android.os.Bundle
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.postDelayed
import androidx.core.view.updatePadding
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.common.Config
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
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortOrder.DATE_ADDED
import xyz.stignarnia.uiModel.SortOrder.NAME
import xyz.stignarnia.uiModel.SortOrder.NEWEST
import xyz.stignarnia.uiModel.SortOrder.RATING
import xyz.stignarnia.uiModel.SortOrder.RUNTIME
import xyz.stignarnia.uiModel.SortOrder.USER_RATING
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiMyMovies.R
import xyz.stignarnia.uiMyMovies.common.layout.CollectionMovieLayoutManagerProvider
import xyz.stignarnia.uiMyMovies.common.layout.CollectionMovieListItemDecoration
import xyz.stignarnia.uiMyMovies.common.recycler.CollectionAdapter
import xyz.stignarnia.uiMyMovies.common.recycler.CollectionListItem.FiltersItem
import xyz.stignarnia.uiMyMovies.common.recycler.CollectionListItem.MovieItem
import xyz.stignarnia.uiMyMovies.databinding.FragmentHiddenMoviesBinding
import xyz.stignarnia.uiMyMovies.filters.CollectionFiltersOrigin.HIDDEN_MOVIES
import xyz.stignarnia.uiMyMovies.filters.genre.CollectionFiltersGenreBottomSheet
import xyz.stignarnia.uiMyMovies.filters.genre.CollectionFiltersGenreBottomSheet.Companion.REQUEST_COLLECTION_FILTERS_GENRE
import xyz.stignarnia.uiMyMovies.main.FollowedMoviesFragment
import xyz.stignarnia.uiMyMovies.main.FollowedMoviesViewModel
import xyz.stignarnia.uiNavigation.java.NavigationArgs
import javax.inject.Inject

@AndroidEntryPoint
class HiddenFragment :
  BaseFragment<HiddenViewModel>(R.layout.fragment_hidden_movies),
  OnScrollResetListener,
  OnSearchClickListener {
  @Inject lateinit var settings: SettingsViewModeRepository

  override val navigationId = R.id.followedMoviesFragment
  private val binding by viewBinding(FragmentHiddenMoviesBinding::bind)

  private val parentViewModel by viewModels<FollowedMoviesViewModel>({ requireParentFragment() })
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
      doAfterLaunch = { viewModel.loadMovies() },
    )
  }

  private fun setupRecycler() {
    layoutManager =
      CollectionMovieLayoutManagerProvider
        .provideLayoutManger(requireContext(), LIST_NORMAL, tabletGridSpanSize)
    adapter =
      CollectionAdapter(
        itemClickListener = { openMovieDetails(it.movie) },
        itemLongClickListener = { openMovieMenu(it.movie) },
        sortChipClickListener = ::openSortOrderDialog,
        genreChipClickListener = ::openGenresDialog,
        missingImageListener = viewModel::loadMissingImage,
        missingTranslationListener = viewModel::loadMissingTranslation,
        upcomingChipVisible = false,
        upcomingChipClickListener = {},
        listChangeListener = {
          binding.hiddenMoviesRecycler.scrollToPosition(0)
          (requireParentFragment() as FollowedMoviesFragment).resetTranslations()
        },
      )
    binding.hiddenMoviesRecycler.apply {
      setHasFixedSize(true)
      adapter = this@HiddenFragment.adapter
      layoutManager = this@HiddenFragment.layoutManager
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
      addItemDecoration(CollectionMovieListItemDecoration(requireContext(), R.dimen.spaceSmall))
    }
  }

  private fun setupInsets() {
    with(binding) {
      root.doOnApplyWindowInsets { _, insets, padding, _ ->
        val tabletOffset = if (isTablet) dimenToPx(R.dimen.spaceMedium) else 0
        val systemInset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        hiddenMoviesContent.updatePadding(top = padding.top + systemInset.top + tabletOffset)
        hiddenMoviesRecycler.updatePadding(
          top = dimenToPx(R.dimen.collectionTabsViewPadding),
          bottom = dimenToPx(R.dimen.myMoviesBottomPadding) + systemInset.bottom,
        )
      }
    }
  }

  private fun render(uiState: HiddenUiState) {
    uiState.run {
      viewMode.let {
        if (adapter?.listViewMode != it) {
          layoutManager =
            CollectionMovieLayoutManagerProvider.provideLayoutManger(
              context = requireContext(),
              viewMode = it,
              gridSpanSize = tabletGridSpanSize,
            )
          adapter?.listViewMode = it
          binding.hiddenMoviesRecycler.let { recycler ->
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
                LIST_NORMAL -> if (isTablet) tabletGridSpanSize else Config.LISTS_GRID_SPAN
              }
            }

            is MovieItem -> {
              1
            }

            else -> {
              throw Error("Unsupported span size!")
            }
          }
        }

        binding.hiddenMoviesEmptyView.root.fadeIf(it.isEmpty() && !isSearching)
      }
      sortOrder?.let { event ->
        event.consume()?.let { openSortOrderDialog(it.first, it.second) }
      }
    }
  }

  private fun openSortOrderDialog(
    order: SortOrder,
    type: SortType,
  ) {
    val options = listOf(NAME, RATING, USER_RATING, RUNTIME, NEWEST, DATE_ADDED)
    val args = SortOrderBottomSheet.createBundle(options, order, type)

    requireParentFragment().setFragmentResultListener(NavigationArgs.REQUEST_SORT_ORDER) { _, bundle ->
      val sortOrder = bundle.requireSerializable<SortOrder>(NavigationArgs.ARG_SELECTED_SORT_ORDER)
      val sortType = bundle.requireSerializable<SortType>(NavigationArgs.ARG_SELECTED_SORT_TYPE)
      viewModel.setSortOrder(sortOrder, sortType)
    }

    navigateTo(R.id.actionFollowedMoviesFragmentToSortOrder, args)
  }

  private fun openGenresDialog() {
    requireParentFragment().setFragmentResultListener(REQUEST_COLLECTION_FILTERS_GENRE) { _, _ ->
      viewModel.loadMovies(resetScroll = true)
    }

    val bundle = CollectionFiltersGenreBottomSheet.createBundle(HIDDEN_MOVIES)
    navigateToSafe(R.id.actionFollowedMoviesFragmentToGenres, bundle)
  }

  private fun openMovieDetails(movie: Movie) {
    (requireParentFragment() as? FollowedMoviesFragment)?.openMovieDetails(movie)
  }

  private fun openMovieMenu(movie: Movie) {
    (requireParentFragment() as? FollowedMoviesFragment)?.openMovieMenu(movie)
  }

  override fun onEnterSearch() {
    isSearching = true
    with(binding.hiddenMoviesRecycler) {
      translationY = dimenToPx(R.dimen.myMoviesSearchLocalOffset).toFloat()
      smoothScrollToPosition(0)
    }
  }

  override fun onExitSearch() {
    isSearching = false
    with(binding.hiddenMoviesRecycler) {
      translationY = 0F
      postDelayed(200) { layoutManager?.scrollToPosition(0) }
    }
  }

  override fun onScrollReset() = binding.hiddenMoviesRecycler.scrollToPosition(0)

  override fun setupBackPressed() = Unit

  override fun onDestroyView() {
    adapter = null
    layoutManager = null
    super.onDestroyView()
  }
}
