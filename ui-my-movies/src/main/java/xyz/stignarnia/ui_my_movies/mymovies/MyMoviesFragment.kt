package xyz.stignarnia.ui_my_movies.mymovies

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
import xyz.stignarnia.ui_base.utilities.extensions.withSpanSizeLookup
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_model.Movie
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortOrder.DATE_ADDED
import xyz.stignarnia.ui_model.SortOrder.NAME
import xyz.stignarnia.ui_model.SortOrder.NEWEST
import xyz.stignarnia.ui_model.SortOrder.RANDOM
import xyz.stignarnia.ui_model.SortOrder.RATING
import xyz.stignarnia.ui_model.SortOrder.RUNTIME
import xyz.stignarnia.ui_model.SortOrder.USER_RATING
import xyz.stignarnia.ui_model.SortType
import xyz.stignarnia.ui_my_movies.R
import xyz.stignarnia.ui_my_movies.databinding.FragmentMyMoviesBinding
import xyz.stignarnia.ui_my_movies.filters.CollectionFiltersOrigin.MY_MOVIES
import xyz.stignarnia.ui_my_movies.filters.genre.CollectionFiltersGenreBottomSheet
import xyz.stignarnia.ui_my_movies.filters.genre.CollectionFiltersGenreBottomSheet.Companion.REQUEST_COLLECTION_FILTERS_GENRE
import xyz.stignarnia.ui_my_movies.main.FollowedMoviesFragment
import xyz.stignarnia.ui_my_movies.main.FollowedMoviesViewModel
import xyz.stignarnia.ui_my_movies.mymovies.recycler.MyMoviesAdapter
import xyz.stignarnia.ui_my_movies.mymovies.recycler.MyMoviesItem.Type.ALL_MOVIES_ITEM
import xyz.stignarnia.ui_my_movies.mymovies.recycler.MyMoviesItem.Type.HEADER
import xyz.stignarnia.ui_my_movies.mymovies.recycler.MyMoviesItem.Type.RECENT_MOVIES
import xyz.stignarnia.ui_my_movies.mymovies.recycler.MyMoviesLayoutManagerProvider
import xyz.stignarnia.ui_my_movies.mymovies.utilities.MyMoviesListItemDecoration
import xyz.stignarnia.ui_navigation.java.NavigationArgs
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MyMoviesFragment :
  BaseFragment<MyMoviesViewModel>(R.layout.fragment_my_movies),
  OnScrollResetListener,
  OnSearchClickListener {

  @Inject lateinit var settings: SettingsViewModeRepository

  override val navigationId = R.id.followedMoviesFragment
  private val binding by viewBinding(FragmentMyMoviesBinding::bind)

  private val parentViewModel by viewModels<FollowedMoviesViewModel>({ requireParentFragment() })
  override val viewModel by viewModels<MyMoviesViewModel>()

  private var adapter: MyMoviesAdapter? = null
  private var layoutManager: LayoutManager? = null
  private var isSearching = false
  private val gridSpanSize by lazy { settings.tabletGridSpanSize }

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
    layoutManager = MyMoviesLayoutManagerProvider.provideLayoutManger(
      context = requireContext(),
      viewMode = LIST_NORMAL,
      gridSpanSize = gridSpanSize,
    )
    adapter = MyMoviesAdapter(
      itemClickListener = { openMovieDetails(it.movie) },
      itemLongClickListener = { openMovieMenu(it.movie) },
      onSortOrderClickListener = ::openSortOrderDialog,
      onGenresClickListener = ::openGenresDialog,
      missingImageListener = { item, force -> viewModel.loadMissingImage(item, force) },
      missingTranslationListener = { viewModel.loadMissingTranslation(it) },
      listChangeListener = {
        layoutManager?.scrollToPosition(0)
        (requireParentFragment() as FollowedMoviesFragment).resetTranslations()
      },
    )
    binding.myMoviesRecycler.apply {
      adapter = this@MyMoviesFragment.adapter
      layoutManager = this@MyMoviesFragment.layoutManager
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
      setHasFixedSize(true)
      addItemDecoration(MyMoviesListItemDecoration(requireContext(), R.dimen.spaceSmall))
    }
  }

  private fun setupInsets() {
    with(binding) {
      root.doOnApplyWindowInsets { _, insets, _, _ ->
        val tabletOffset = if (isTablet) dimenToPx(R.dimen.spaceMedium) else 0
        val systemInset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        myMoviesRoot.updatePadding(top = systemInset.top + tabletOffset)
        myMoviesRecycler.updatePadding(
          top = dimenToPx(R.dimen.myMoviesTabsViewPadding),
          bottom = dimenToPx(R.dimen.myMoviesBottomPadding) + systemInset.bottom,
        )
      }
    }
  }

  private fun render(uiState: MyMoviesUiState) {
    uiState.run {
      with(binding) {
        viewMode.let {
          if (adapter?.listViewMode != it) {
            val state = myMoviesRecycler.layoutManager?.onSaveInstanceState()
            layoutManager = MyMoviesLayoutManagerProvider.provideLayoutManger(requireContext(), it, gridSpanSize)
            adapter?.listViewMode = it
            myMoviesRecycler.let { recycler ->
              recycler.layoutManager = layoutManager
              recycler.adapter = adapter
              recycler.layoutManager?.onRestoreInstanceState(state)
            }
          }
        }
        items?.let {
          val notifyChange = resetScroll?.consume() == true
          adapter?.setItems(it, notifyChange)
          (layoutManager as? GridLayoutManager)?.withSpanSizeLookup { pos ->
            val item = adapter?.getItems()?.get(pos)
            when (item?.type) {
              RECENT_MOVIES, HEADER -> {
                when (viewMode) {
                  LIST_NORMAL -> if (isTablet) gridSpanSize else LISTS_GRID_SPAN
                }
              }
              ALL_MOVIES_ITEM -> {
                1
              }
              null -> {
                throw Error("Unsupported span size!")
              }
            }
          }
          myMoviesEmptyView.root.fadeIf(showEmptyView && !isSearching)
        }
      }
    }
  }

  private fun openMovieDetails(movie: Movie) {
    (requireParentFragment() as? FollowedMoviesFragment)?.openMovieDetails(movie)
  }

  private fun openMovieMenu(movie: Movie) {
    (requireParentFragment() as? FollowedMoviesFragment)?.openMovieMenu(movie)
  }

  private fun openSortOrderDialog(
    order: SortOrder,
    type: SortType,
  ) {
    val options = listOf(NAME, RATING, USER_RATING, RUNTIME, NEWEST, DATE_ADDED, RANDOM)
    val args = SortOrderBottomSheet.createBundle(options, order, type)

    requireParentFragment().setFragmentResultListener(NavigationArgs.REQUEST_SORT_ORDER) { _, bundle ->
      val sortOrder = bundle.getSerializable(NavigationArgs.ARG_SELECTED_SORT_ORDER) as SortOrder
      val sortType = bundle.getSerializable(NavigationArgs.ARG_SELECTED_SORT_TYPE) as SortType
      viewModel.setSortOrder(sortOrder, sortType)
    }

    navigateTo(R.id.actionFollowedMoviesFragmentToSortOrder, args)
  }

  private fun openGenresDialog() {
    requireParentFragment().setFragmentResultListener(REQUEST_COLLECTION_FILTERS_GENRE) { _, _ ->
      viewModel.loadMovies()
    }
    val bundle = CollectionFiltersGenreBottomSheet.createBundle(MY_MOVIES)
    navigateToSafe(R.id.actionFollowedMoviesFragmentToGenres, bundle)
  }

  override fun onEnterSearch() {
    isSearching = true
    with(binding.myMoviesRecycler) {
      translationY = dimenToPx(R.dimen.myMoviesSearchLocalOffset).toFloat()
      smoothScrollToPosition(0)
    }
  }

  override fun onExitSearch() {
    isSearching = false
    with(binding.myMoviesRecycler) {
      translationY = 0F
      postDelayed(200) { layoutManager?.scrollToPosition(0) }
    }
  }

  override fun onScrollReset() = binding.myMoviesRecycler.scrollToPosition(0)

  override fun setupBackPressed() = Unit

  override fun onDestroyView() {
    adapter = null
    layoutManager = null
    super.onDestroyView()
  }
}
