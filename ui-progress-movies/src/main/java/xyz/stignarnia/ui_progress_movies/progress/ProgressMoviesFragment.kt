package xyz.stignarnia.ui_progress_movies.progress

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import xyz.stignarnia.repository.settings.SettingsViewModeRepository
import xyz.stignarnia.ui_base.BaseFragment
import xyz.stignarnia.ui_base.common.OnScrollResetListener
import xyz.stignarnia.ui_base.common.OnSearchClickListener
import xyz.stignarnia.ui_base.common.WidgetsProvider
import xyz.stignarnia.ui_base.common.sheets.sort_order.SortOrderBottomSheet
import xyz.stignarnia.ui_base.utilities.NavigationHost
import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_base.utilities.events.MessageEvent
import xyz.stignarnia.ui_base.utilities.extensions.add
import xyz.stignarnia.ui_base.utilities.extensions.dimenToPx
import xyz.stignarnia.ui_base.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.ui_base.utilities.extensions.fadeIf
import xyz.stignarnia.ui_base.utilities.extensions.fadeIn
import xyz.stignarnia.ui_base.utilities.extensions.gone
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.withSpanSizeLookup
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_model.ProgressDateSelectionType.ALWAYS_ASK
import xyz.stignarnia.ui_model.ProgressDateSelectionType.NOW
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortOrder.DATE_ADDED
import xyz.stignarnia.ui_model.SortOrder.NAME
import xyz.stignarnia.ui_model.SortOrder.NEWEST
import xyz.stignarnia.ui_model.SortOrder.RANDOM
import xyz.stignarnia.ui_model.SortOrder.RATING
import xyz.stignarnia.ui_model.SortOrder.RUNTIME
import xyz.stignarnia.ui_model.SortOrder.USER_RATING
import xyz.stignarnia.ui_model.SortType
import xyz.stignarnia.ui_navigation.java.NavigationArgs.ARG_SELECTED_SORT_ORDER
import xyz.stignarnia.ui_navigation.java.NavigationArgs.ARG_SELECTED_SORT_TYPE
import xyz.stignarnia.ui_navigation.java.NavigationArgs.REQUEST_SORT_ORDER
import xyz.stignarnia.ui_progress_movies.R
import xyz.stignarnia.ui_progress_movies.databinding.FragmentProgressMoviesBinding
import xyz.stignarnia.ui_progress_movies.helpers.ProgressMoviesLayoutManagerProvider
import xyz.stignarnia.ui_progress_movies.main.MovieCheckActionUiEvent
import xyz.stignarnia.ui_progress_movies.main.ProgressMoviesMainFragment
import xyz.stignarnia.ui_progress_movies.main.ProgressMoviesMainViewModel
import xyz.stignarnia.ui_progress_movies.main.RequestWidgetsUpdate
import xyz.stignarnia.ui_progress_movies.progress.recycler.ProgressMovieListItem.FiltersItem
import xyz.stignarnia.ui_progress_movies.progress.recycler.ProgressMovieListItem.HeaderItem
import xyz.stignarnia.ui_progress_movies.progress.recycler.ProgressMovieListItem.MovieItem
import xyz.stignarnia.ui_progress_movies.progress.recycler.ProgressMoviesAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProgressMoviesFragment :
  BaseFragment<ProgressMoviesViewModel>(R.layout.fragment_progress_movies),
  OnSearchClickListener,
  OnScrollResetListener {

  private companion object {
  }

  @Inject lateinit var settings: SettingsViewModeRepository

  private val parentViewModel by viewModels<ProgressMoviesMainViewModel>({ requireParentFragment() })
  override val viewModel by viewModels<ProgressMoviesViewModel>()

  private val binding by viewBinding(FragmentProgressMoviesBinding::bind)

  private var adapter: ProgressMoviesAdapter? = null
  private var layoutManager: LayoutManager? = null
  private var statusBarHeight = 0
  private var isSearching = false

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    setupRecycler()
    setupInsets()

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        with(parentViewModel) {
          launch { uiState.collect { viewModel.onParentState(it) } }
        }
        with(viewModel) {
          launch { uiState.collect { render(it) } }
          launch { messageFlow.collect { showSnack(it) } }
          launch { eventFlow.collect { handleEvent(it) } }
        }
      }
    }
  }

  private fun setupView() {
    with(binding) {
      progressMoviesEmptyView.progressMoviesEmptyDiscoverButton.onClick {
        (requireActivity() as NavigationHost).navigateToDiscover()
      }
    }
  }

  private fun setupRecycler() {
    val gridSpanSize = settings.tabletGridSpanSize
    layoutManager = ProgressMoviesLayoutManagerProvider.provideLayoutManger(requireContext(), gridSpanSize)
    (layoutManager as? GridLayoutManager)?.run {
      withSpanSizeLookup { position ->
        when (adapter?.getItems()?.get(position)) {
          is HeaderItem -> gridSpanSize
          is FiltersItem -> gridSpanSize
          is MovieItem -> 1
          else -> throw IllegalStateException()
        }
      }
    }
    adapter = ProgressMoviesAdapter(
      itemClickListener = { requireMainFragment().openMovieDetails(it.movie) },
      itemLongClickListener = { requireMainFragment().openMovieMenu(it.movie) },
      sortChipClickListener = ::openSortOrderDialog,
      missingImageListener = viewModel::findMissingImage,
      missingTranslationListener = viewModel::findMissingTranslation,
      checkClickListener = { viewModel.onMovieChecked(it.movie) },
      listChangeListener = {
        requireMainFragment().resetTranslations()
        layoutManager?.scrollToPosition(0)
      },
    )
    binding.progressMoviesMainRecycler.apply {
      adapter = this@ProgressMoviesFragment.adapter
      layoutManager = this@ProgressMoviesFragment.layoutManager
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
      setHasFixedSize(true)
    }
  }

  private fun setupOverscroll() {
    if (view == null) return
    with(binding.progressMoviesOverscroll) {
      onTriggered = { onOverscrollTriggered() }
      attach(binding.progressMoviesMainRecycler, viewLifecycleOwner)
    }
  }

  /**
   * The pull completed. Runs a backup, and a sync with the user's other
   * devices, when one can actually run - and says so either way, because a
   * gesture that animates and then does nothing is worse than no gesture.
   */
  private fun onOverscrollTriggered() {
    val started = viewModel.startBackupNow()
    val message = if (started) R.string.textBackupStarted else R.string.textBackupNotConfigured
    showSnack(MessageEvent.Info(message))
  }

  private fun setupInsets() {
    with(binding) {
      root.doOnApplyWindowInsets { _, insets, padding, _ ->
        val tabletOffset = if (isTablet) dimenToPx(R.dimen.spaceMedium) else 0
        val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        statusBarHeight = systemInsets.top + tabletOffset
        progressMoviesMainRecycler.updatePadding(
          top = statusBarHeight + dimenToPx(R.dimen.progressMoviesTabsViewPadding),
          bottom = systemInsets.bottom + dimenToPx(R.dimen.bottomNavigationHeightPadded),
        )
        (progressMoviesEmptyView.rootLayout.layoutParams as ViewGroup.MarginLayoutParams)
          .updateMargins(top = statusBarHeight + dimenToPx(R.dimen.spaceBig))
        (progressMoviesOverscroll.layoutParams as ViewGroup.MarginLayoutParams)
          .updateMargins(top = statusBarHeight + dimenToPx(R.dimen.progressMoviesOverscrollPadding))
      }
    }
  }

  private fun openSortOrderDialog(
    order: SortOrder,
    type: SortType,
  ) {
    val options = listOf(NAME, RATING, USER_RATING, RUNTIME, NEWEST, DATE_ADDED, RANDOM)
    val args = SortOrderBottomSheet.createBundle(options, order, type)

    requireParentFragment().setFragmentResultListener(REQUEST_SORT_ORDER) { _, bundle ->
      val sortOrder = bundle.getSerializable(ARG_SELECTED_SORT_ORDER) as SortOrder
      val sortType = bundle.getSerializable(ARG_SELECTED_SORT_TYPE) as SortType
      viewModel.setSortOrder(sortOrder, sortType)
    }

    navigateTo(R.id.actionProgressMoviesFragmentToSortOrder, args)
  }

  override fun onEnterSearch() {
    isSearching = true

    binding.progressMoviesMainRecycler.translationY = dimenToPx(R.dimen.progressMoviesSearchLocalOffset).toFloat()
    binding.progressMoviesMainRecycler.smoothScrollToPosition(0)

    binding.progressMoviesOverscroll.detach()
  }

  override fun onExitSearch() {
    isSearching = false

    binding.progressMoviesMainRecycler.translationY = 0F
    binding.progressMoviesMainRecycler.smoothScrollToPosition(0)

    setupOverscroll()
  }

  override fun onScrollReset() = binding.progressMoviesMainRecycler.smoothScrollToPosition(0)

  private fun handleEvent(event: Event<*>) {
    when (event) {
      is MovieCheckActionUiEvent -> {
        when (event.dateSelectionType) {
          ALWAYS_ASK -> requireMainFragment().openDateSelectionDialog(event.movie)
          NOW -> parentViewModel.setWatchedMovie(event.movie)
        }
      }
      is RequestWidgetsUpdate -> {
        (requireAppContext() as WidgetsProvider).requestMoviesWidgetsUpdate()
      }
    }
  }

  private fun render(uiState: ProgressMoviesUiState) {
    uiState.run {
      items?.let {
        val resetScroll = scrollReset?.consume() == true
        adapter?.setItems(it, resetScroll)
        renderFiltersEmpty(uiState)
        binding.progressMoviesEmptyView.rootLayout.fadeIf(items.isEmpty() && !isSearching)
        binding.progressMoviesMainRecycler
          .fadeIn(
            duration = 200,
            withHardware = true,
          ).add(animations)
      }
      isOverScrollEnabled.let {
        if (it) {
          setupOverscroll()
        } else {
          binding.progressMoviesOverscroll.detach()
        }
      }
      sortOrder?.let { event -> event.consume()?.let { openSortOrderDialog(it.first, it.second) } }
    }
  }

  private fun renderFiltersEmpty(uiState: ProgressMoviesUiState) {
    val items = uiState.items ?: emptyList()
    if (isSearching) {
      binding.progressMoviesEmptyFilterView.fadeIf(items.isEmpty(), duration = 200)
      return
    }
    binding.progressMoviesEmptyFilterView.gone()
  }

  private fun requireMainFragment() = (requireParentFragment() as ProgressMoviesMainFragment)

  override fun setupBackPressed() = Unit

  override fun onDestroyView() {
    adapter = null
    layoutManager = null
    super.onDestroyView()
  }
}
