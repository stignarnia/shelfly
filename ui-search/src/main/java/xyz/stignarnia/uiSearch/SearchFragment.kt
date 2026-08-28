package xyz.stignarnia.uiSearch

import android.graphics.drawable.Animatable
import android.os.Bundle
import android.text.Editable
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.common.Mode
import xyz.stignarnia.repository.settings.SettingsViewModeRepository
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.common.sheets.contextMenu.ContextMenuBottomSheet
import xyz.stignarnia.uiBase.common.sheets.sortOrder.SortOrderBottomSheet
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
import xyz.stignarnia.uiBase.utilities.extensions.navigateBack
import xyz.stignarnia.uiBase.utilities.extensions.navigateToSafe
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.requireSerializable
import xyz.stignarnia.uiBase.utilities.extensions.shake
import xyz.stignarnia.uiBase.utilities.extensions.showKeyboard
import xyz.stignarnia.uiBase.utilities.extensions.visible
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiModel.RecentSearch
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiNavigation.java.NavigationArgs
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_MOVIE_ID
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_SELECTED_SORT_ORDER
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_SELECTED_SORT_TYPE
import xyz.stignarnia.uiNavigation.java.NavigationArgs.ARG_SHOW_ID
import xyz.stignarnia.uiNavigation.java.NavigationArgs.REQUEST_SORT_ORDER
import xyz.stignarnia.uiSearch.databinding.FragmentSearchBinding
import xyz.stignarnia.uiSearch.recycler.SearchAdapter
import xyz.stignarnia.uiSearch.recycler.SearchListItem
import xyz.stignarnia.uiSearch.recycler.suggestions.SuggestionAdapter
import xyz.stignarnia.uiSearch.utilities.SearchLayoutManagerProvider
import xyz.stignarnia.uiSearch.utilities.TextWatcherAdapter
import xyz.stignarnia.uiSearch.views.RecentSearchView
import javax.inject.Inject

@AndroidEntryPoint
class SearchFragment :
  BaseFragment<SearchViewModel>(R.layout.fragment_search),
  TextWatcherAdapter {
  companion object {
    private const val ARG_HEADER_TRANSLATION = "ARG_HEADER_TRANSLATION"
  }

  @Inject lateinit var settings: SettingsViewModeRepository

  override val navigationId = R.id.searchFragment

  override val viewModel by viewModels<SearchViewModel>()
  private val binding by viewBinding(FragmentSearchBinding::bind)

  private var adapter: SearchAdapter? = null
  private var suggestionsAdapter: SuggestionAdapter? = null
  private var layoutManager: LayoutManager? = null
  private var suggestionsLayoutManager: LayoutManager? = null

  private var headerTranslation = 0F

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    savedInstanceState?.let {
      headerTranslation = it.getFloat(ARG_HEADER_TRANSLATION)
    }
    return super.onCreateView(inflater, container, savedInstanceState)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    enableUi()
    setupView()
    setupRecycler()
    setupSuggestionsRecycler()
    setupInsets()

    if (savedInstanceState == null && !isInitialized) {
      isInitialized = true
    }

    launchAndRepeatStarted(
      { viewModel.uiState.collect { render(it) } },
      { viewModel.messageFlow.collect { showSnack(it) } },
    )
  }

  override fun onPause() {
    enableUi()
    headerTranslation = binding.searchFiltersView.translationY
    super.onPause()
  }

  override fun onStop() {
    viewModel.clearSuggestions()
    with(binding) {
      searchViewLayout.binding.searchViewInput.removeTextChangedListener(this@SearchFragment)
      searchViewLayout.binding.searchViewInput.setText("")
    }
    super.onStop()
  }

  override fun onDestroyView() {
    adapter = null
    layoutManager = null
    super.onDestroyView()
  }

  private fun setupView() {
    with(binding) {
      searchViewLayout.binding.searchViewInput.visible()
      searchViewLayout.binding.searchViewText.gone()
      (searchViewLayout.binding.searchViewIcon.drawable as Animatable).start()
      searchViewLayout.settingsIconVisible = false

      viewModel.preloadSuggestions()

      if (!isInitialized) {
        searchViewLayout.binding.searchViewInput.showKeyboard()
        searchViewLayout.binding.searchViewInput.requestFocus()
        viewModel.loadRecentSearches()
      }

      searchViewLayout.binding.searchViewInput.run {
        addTextChangedListener(this@SearchFragment)
        setOnEditorActionListener { textView, id, _ ->
          if (id == EditorInfo.IME_ACTION_SEARCH) {
            val query = textView.text.toString()
            return@setOnEditorActionListener onSearchQuery(query)
          }
          true
        }
        setOnKeyListener { _, keyCode, keyEvent ->
          if (keyEvent.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
            val query = text.toString()
            return@setOnKeyListener onSearchQuery(query)
          }
          false
        }
      }

      searchViewLayout.binding.searchViewIcon.onClick {
        searchViewLayout.binding.searchViewInput.hideKeyboard()
        navigateBack()
      }

      with(searchFiltersView) {
        onChipsChangeListener = viewModel::setFilters
        onSortClickListener = ::openSortingDialog
        translationY = headerTranslation
      }
    }
  }

  private fun setupRecycler() {
    with(binding) {
      layoutManager = SearchLayoutManagerProvider.provideLayoutManger(requireContext(), settings.tabletGridSpanSize)
      adapter =
        SearchAdapter(
          itemClickListener = { openShowDetails(it) },
          itemLongClickListener = { openContextMenu(it) },
          missingImageListener = { ids, force -> viewModel.loadMissingImage(ids, force) },
          listChangeListener = { searchRecycler.scrollToPosition(0) },
        )
      searchRecycler.apply {
        setHasFixedSize(true)
        adapter = this@SearchFragment.adapter
        layoutManager = this@SearchFragment.layoutManager
        itemAnimator = null
        clearOnScrollListeners()
        addOnScrollListener(
          object : RecyclerView.OnScrollListener() {
            override fun onScrolled(
              recyclerView: RecyclerView,
              dx: Int,
              dy: Int,
            ) {
              val value = searchFiltersView.translationY - dy
              searchFiltersView.translationY = value.coerceAtMost(0F)
            }
          },
        )
      }
    }
  }

  private fun setupSuggestionsRecycler() {
    suggestionsLayoutManager =
      SearchLayoutManagerProvider
        .provideLayoutManger(requireContext(), settings.tabletGridSpanSize)
    suggestionsAdapter =
      SuggestionAdapter(
        itemClickListener = {
          val query =
            if (it.translation?.title?.isNotBlank() == true) {
              it.translation.title
            } else {
              it.title
            }
          viewModel.saveRecentSearch(query)
          openDetails(it)
        },
        missingImageListener = { ids, force -> viewModel.loadMissingSuggestionImage(ids, force) },
        missingTranslationListener = { viewModel.loadMissingSuggestionTranslation(it) },
      )
    binding.suggestionsRecycler.apply {
      adapter = this@SearchFragment.suggestionsAdapter
      layoutManager = this@SearchFragment.suggestionsLayoutManager
      itemAnimator = null
    }
  }

  private fun setupInsets() {
    with(binding) {
      searchRoot.doOnApplyWindowInsets { view, insets, padding, _ ->
        val tabletOffset = if (isTablet) dimenToPx(R.dimen.spaceMedium) else 0
        val inset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(top = inset.top + tabletOffset)
        searchRecycler.updatePadding(bottom = inset.bottom + padding.bottom)
      }
    }
  }

  private fun onSearchQuery(query: String): Boolean {
    with(binding) {
      if (query.trim().isBlank()) {
        searchViewLayout.shake()
        return true
      }
      viewModel.search(query)
      searchViewLayout.binding.searchViewInput.hideKeyboard()
      searchViewLayout.binding.searchViewInput.clearFocus()
      return true
    }
  }

  private fun openSortingDialog(
    order: SortOrder,
    type: SortType,
  ) {
    val options = listOf(SortOrder.RANK, SortOrder.NAME, SortOrder.NEWEST)
    val args = SortOrderBottomSheet.createBundle(options, order, type)

    setFragmentResultListener(REQUEST_SORT_ORDER) { _, bundle ->
      val sortOrder = bundle.requireSerializable<SortOrder>(ARG_SELECTED_SORT_ORDER)
      val sortType = bundle.requireSerializable<SortType>(ARG_SELECTED_SORT_TYPE)
      viewModel.setSortOrder(sortOrder, sortType)
    }

    navigateToSafe(R.id.actionSearchFragmentToSortOrder, args)
  }

  private fun openShowDetails(item: SearchListItem) {
    disableUi()
    binding.searchRoot
      .fadeOut(150) {
        openDetails(item)
      }.add(animations)
  }

  private fun openDetails(item: SearchListItem) {
    if (item.isShow) {
      val bundle = Bundle().apply { putLong(ARG_SHOW_ID, item.show.tmdbId) }
      navigateToSafe(R.id.actionSearchFragmentToShowDetailsFragment, bundle)
    } else if (item.isMovie) {
      val bundle = Bundle().apply { putLong(ARG_MOVIE_ID, item.movie.tmdbId) }
      navigateToSafe(R.id.actionSearchFragmentToMovieDetailsFragment, bundle)
    }
  }

  private fun openContextMenu(item: SearchListItem) {
    setFragmentResultListener(NavigationArgs.REQUEST_ITEM_MENU) { requestKey, _ ->
      if (requestKey == NavigationArgs.REQUEST_ITEM_MENU) {
        viewModel.refreshFollowState(item)
      }
      clearFragmentResultListener(NavigationArgs.REQUEST_ITEM_MENU)
    }
    if (item.isShow) {
      val bundle = ContextMenuBottomSheet.createBundle(item.show.ids.tmdb)
      navigateToSafe(R.id.actionSearchFragmentToShowItemMenu, bundle)
    } else if (item.isMovie) {
      val bundle = ContextMenuBottomSheet.createBundle(item.movie.ids.tmdb)
      navigateToSafe(R.id.actionSearchFragmentToMovieItemMenu, bundle)
    }
  }

  private fun render(uiState: SearchUiState) {
    uiState.run {
      with(binding) {
        searchItems?.let {
          val resetScroll = resetScroll?.consume() == true
          adapter?.setItems(it, resetScroll)
          if (searchItemsAnimate?.consume() == true) {
            searchRecycler.scheduleLayoutAnimation()
          }
          if (resetScroll) {
            searchFiltersView.translationY = 0F
          }
        }
        recentSearchItems?.let { renderRecentSearches(it) }
        suggestionsItems?.let {
          suggestionsAdapter?.setItems(it)
          suggestionsRecycler.visibleIf(it.isNotEmpty())
        }
        searchOptions?.let {
          searchFiltersView.setTypes(it.filters)
          searchFiltersView.setSorting(it.sortOrder, it.sortType)
        }
        isSearching.let {
          searchOverscroll.setRunning(it)
          searchViewLayout.isEnabled = !it
        }
        sortOrder?.let { event ->
          event.consume()?.let { openSortingDialog(it.first, it.second) }
        }
        isMoviesEnabled.let { isEnabled ->
          val types =
            mutableListOf(Mode.SHOWS).apply {
              if (isEnabled) add(Mode.MOVIES)
            }
          searchFiltersView.setEnabledTypes(types)
        }
        searchEmptyView.fadeIf(isEmpty)
        searchInitialView.fadeIf(isInitial)
        searchFiltersView.visibleIf(isFiltersVisible)
      }
    }
  }

  private fun renderRecentSearches(it: List<RecentSearch>) {
    with(binding) {
      if (it.isEmpty()) {
        searchRecentsClearButton.gone()
        searchRecentsLayout.removeAllViews()
        searchRecentsLayout.gone()
        return
      }

      searchRecentsLayout.fadeIn()
      searchRecentsClearButton.fadeIn()
      searchRecentsClearButton.onClick { viewModel.clearRecentSearches() }

      val paddingH = requireContext().dimenToPx(R.dimen.screenMarginHorizontal)
      val paddingV = requireContext().dimenToPx(R.dimen.spaceMedium)

      searchRecentsLayout.removeAllViews()
      it.forEach { item ->
        val view =
          RecentSearchView(requireContext()).apply {
            setPadding(paddingH, paddingV, paddingH, paddingV)
            bind(item)
            onClick {
              viewModel.search(item.text)
              searchViewLayout.binding.searchViewInput.setText(item.text)
            }
          }
        searchRecentsLayout.addView(view)
      }
    }
  }

  override fun afterTextChanged(text: Editable?) {
    viewModel.loadSuggestions(text.toString())
  }
}
