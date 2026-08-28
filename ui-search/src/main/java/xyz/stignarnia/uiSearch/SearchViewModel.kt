package xyz.stignarnia.uiSearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.Config.SEARCH_RECENTS_AMOUNT
import xyz.stignarnia.common.Mode
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiBase.utilities.events.MessageEvent
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.uiBase.utilities.extensions.combine
import xyz.stignarnia.uiBase.utilities.extensions.findReplace
import xyz.stignarnia.uiBase.viewmodel.ChannelsDelegate
import xyz.stignarnia.uiBase.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.RecentSearch
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import xyz.stignarnia.uiSearch.cases.SearchFiltersCase
import xyz.stignarnia.uiSearch.cases.SearchInvalidateItemCase
import xyz.stignarnia.uiSearch.cases.SearchQueryCase
import xyz.stignarnia.uiSearch.cases.SearchRecentsCase
import xyz.stignarnia.uiSearch.cases.SearchSortingCase
import xyz.stignarnia.uiSearch.cases.SearchSuggestionsCase
import xyz.stignarnia.uiSearch.cases.SearchTranslationsCase
import xyz.stignarnia.uiSearch.recycler.SearchListItem
import xyz.stignarnia.uiSearch.utilities.SearchOptions
import javax.inject.Inject

@HiltViewModel
class SearchViewModel
  @Inject
  constructor(
    private val searchQueryCase: SearchQueryCase,
    private val searchFiltersCase: SearchFiltersCase,
    private val searchSortingCase: SearchSortingCase,
    private val searchTranslationsCase: SearchTranslationsCase,
    private val searchInvalidateCase: SearchInvalidateItemCase,
    private val recentSearchesCase: SearchRecentsCase,
    private val suggestionsCase: SearchSuggestionsCase,
    private val showsImagesProvider: ShowImagesProvider,
    private val moviesImagesProvider: MovieImagesProvider,
  ) : ViewModel(),
    ChannelsDelegate by DefaultChannelsDelegate() {
    private val searchItemsState = MutableStateFlow<List<SearchListItem>?>(null)
    private val searchItemsAnimateEvent = MutableStateFlow<Event<Boolean>?>(null)
    private val recentSearchItemsState = MutableStateFlow<List<RecentSearch>?>(null)
    private val suggestionsItemsState = MutableStateFlow<List<SearchListItem>?>(null)
    private val searchOptionsState = MutableStateFlow(SearchOptions())
    private val searchingState = MutableStateFlow(false)
    private val emptyState = MutableStateFlow(false)
    private val initialState = MutableStateFlow(false)
    private val filtersVisibleState = MutableStateFlow(false)
    private val moviesEnabledState = MutableStateFlow(true)
    private val resetScrollEvent = MutableStateFlow<Event<Boolean>?>(null)

    private var isSearching = false
    private var suggestionsJob: Job? = null
    private var currentSearch: List<SearchListItem>? = null

    fun preloadSuggestions() {
      viewModelScope.launch {
        suggestionsCase.preloadCache()
      }
    }

    fun loadRecentSearches() {
      viewModelScope.launch {
        val searches = recentSearchesCase.getRecentSearches(SEARCH_RECENTS_AMOUNT)
        recentSearchItemsState.value = searches
        initialState.value = searches.isEmpty()
      }
    }

    fun clearRecentSearches() {
      viewModelScope.launch {
        recentSearchesCase.clearRecentSearches()
        recentSearchItemsState.value = emptyList()
        initialState.value = true
      }
    }

    fun search(query: String) {
      fun setInitialState() {
        searchItemsState.value = emptyList()
        searchItemsAnimateEvent.value = Event(false)
        searchingState.value = true
        emptyState.value = false
        initialState.value = false
        filtersVisibleState.value = false
        suggestionsItemsState.value = emptyList()
        searchOptionsState.value = SearchOptions()
        recentSearchItemsState.value = emptyList()
        currentSearch = null
      }

      fun setResultsState(
        items: List<SearchListItem>,
        isMoviesEnabled: Boolean,
      ) {
        searchItemsState.value = items
        searchItemsAnimateEvent.value = Event(true)
        searchingState.value = false
        emptyState.value = items.isEmpty()
        initialState.value = false
        filtersVisibleState.value = items.isNotEmpty()
        moviesEnabledState.value = isMoviesEnabled
        suggestionsItemsState.value = emptyList()
        resetScrollEvent.value = Event(true)
      }

      val trimmed = query.trim()
      if (trimmed.isEmpty()) return

      viewModelScope.launch {
        try {
          isSearching = true
          setInitialState()

          val results = searchQueryCase.searchByQuery(trimmed)
          val isMoviesEnabled = searchFiltersCase.isMoviesEnabled
          val items = results.also { currentSearch = it }
          recentSearchesCase.saveRecentSearch(trimmed)

          setResultsState(items, isMoviesEnabled)
        } catch (t: Throwable) {
          onError()
        } finally {
          isSearching = false
        }
      }
    }

    fun saveRecentSearch(query: String) {
      if (query.trim().isBlank()) return
      viewModelScope.launch {
        recentSearchesCase.saveRecentSearch(query)
      }
    }

    fun setFilters(filters: List<Mode>) {
      val currentOptions = searchOptionsState.value
      if (currentOptions.filters == filters) return
      val newOptions = currentOptions.copy(filters = filters)

      searchOptionsState.value = newOptions
      searchItemsState.value =
        currentSearch
          ?.filter { searchFiltersCase.filter(newOptions, it) }
          ?.sortedWith(searchSortingCase.sort(newOptions))
      resetScrollEvent.value = Event(true)
    }

    fun setSortOrder(
      sortOrder: SortOrder,
      sortType: SortType,
    ) {
      val currentOptions = searchOptionsState.value
      if (currentOptions.sortOrder == sortOrder && currentOptions.sortType == sortType) {
        return
      }
      val newOptions = currentOptions.copy(sortOrder = sortOrder, sortType = sortType)

      searchOptionsState.value = newOptions
      searchItemsState.value =
        currentSearch
          ?.filter { searchFiltersCase.filter(newOptions, it) }
          ?.sortedWith(searchSortingCase.sort(newOptions))
      resetScrollEvent.value = Event(true)
    }

    fun loadSuggestions(query: String) {
      suggestionsJob?.cancel()

      if (query.trim().length < 2 || isSearching) {
        clearSuggestions()
        return
      }

      suggestionsJob =
        viewModelScope.launch {
          val results = suggestionsCase.loadSuggestions(query)
          suggestionsItemsState.value = results
        }
    }

    fun clearSuggestions() {
      suggestionsItemsState.value = emptyList()
    }

    fun refreshFollowState(item: SearchListItem) {
      val currentItems = searchItemsState.value?.toMutableList() ?: mutableListOf()
      currentItems.find { it.id == item.id }?.let {
        viewModelScope.launch {
          val (isFollowed, isWatchlist) = searchInvalidateCase.checkFollowedState(it)
          updateItem(it.copy(isFollowed = isFollowed, isWatchlist = isWatchlist))
        }
      }
    }

    fun loadMissingImage(
      item: SearchListItem,
      force: Boolean,
    ) {
      viewModelScope.launch {
        updateItem(item.copy(isLoading = true))
        try {
          val image =
            if (item.isShow) {
              showsImagesProvider.loadRemoteImage(item.show, item.image.type, force)
            } else {
              moviesImagesProvider.loadRemoteImage(item.movie, item.image.type, force)
            }
          updateItem(item.copy(isLoading = false, image = image))
        } catch (t: Throwable) {
          updateItem(item.copy(isLoading = false, image = Image.createUnavailable(item.image.type)))
        }
      }
    }

    fun loadMissingSuggestionImage(
      item: SearchListItem,
      force: Boolean,
    ) {
      viewModelScope.launch {
        updateSuggestionsItem(item.copy(isLoading = true))
        try {
          val image =
            if (item.isShow) {
              showsImagesProvider.loadRemoteImage(item.show, item.image.type, force)
            } else {
              moviesImagesProvider.loadRemoteImage(item.movie, item.image.type, force)
            }
          updateSuggestionsItem(item.copy(isLoading = false, image = image))
        } catch (t: Throwable) {
          updateSuggestionsItem(item.copy(isLoading = false, image = Image.createUnavailable(item.image.type)))
        }
      }
    }

    fun loadMissingSuggestionTranslation(item: SearchListItem) {
      if (item.translation != null || searchTranslationsCase.getLanguage() == Config.DEFAULT_LANGUAGE) {
        return
      }
      viewModelScope.launch {
        try {
          val translation =
            if (item.isShow) {
              searchTranslationsCase.loadTranslation(item.show)
            } else {
              searchTranslationsCase.loadTranslation(item.movie)
            }
          updateSuggestionsItem(item.copy(translation = translation))
        } catch (error: Throwable) {
          Timber.e(error)
        }
      }
    }

    private fun updateItem(new: SearchListItem) {
      val currentItems = uiState.value.searchItems?.toMutableList()
      currentItems?.run {
        findReplace(new) { it.isSameAs(new) }
      }
      searchItemsState.value = currentItems
    }

    private fun updateSuggestionsItem(new: SearchListItem) {
      val currentItems = uiState.value.suggestionsItems?.toMutableList()
      currentItems?.run { findReplace(new) { it.isSameAs(new) } }
      suggestionsItemsState.value = currentItems
    }

    private suspend fun onError() {
      searchingState.value = false
      emptyState.value = false
      messageChannel.send(MessageEvent.Error(R.string.errorCouldNotLoadSearchResults))
    }

    override fun onCleared() {
      suggestionsCase.clearCache()
    }

    val uiState =
      combine(
        searchItemsState,
        searchItemsAnimateEvent,
        recentSearchItemsState,
        suggestionsItemsState,
        searchingState,
        emptyState,
        initialState,
        moviesEnabledState,
        resetScrollEvent,
        searchOptionsState,
        filtersVisibleState,
      ) { s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11 ->
        SearchUiState(
          searchItems = s1,
          searchItemsAnimate = s2,
          recentSearchItems = s3,
          suggestionsItems = s4,
          isSearching = s5,
          isEmpty = s6,
          isInitial = s7,
          isMoviesEnabled = s8,
          resetScroll = s9,
          searchOptions = s10,
          isFiltersVisible = s11,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
        initialValue = SearchUiState(),
      )
  }
