package xyz.stignarnia.ui_lists.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import xyz.stignarnia.common.Config
import xyz.stignarnia.common.Mode
import xyz.stignarnia.common.Mode.MOVIES
import xyz.stignarnia.common.Mode.SHOWS
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.ui_base.common.ListViewMode
import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_base.utilities.events.MessageEvent
import xyz.stignarnia.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.ui_base.utilities.extensions.combine
import xyz.stignarnia.ui_base.utilities.extensions.findReplace
import xyz.stignarnia.ui_base.viewmodel.ChannelsDelegate
import xyz.stignarnia.ui_base.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.ui_lists.R
import xyz.stignarnia.ui_lists.details.cases.ListDetailsItemsCase
import xyz.stignarnia.ui_lists.details.cases.ListDetailsMainCase
import xyz.stignarnia.ui_lists.details.cases.ListDetailsSortCase
import xyz.stignarnia.ui_lists.details.cases.ListDetailsTipsCase
import xyz.stignarnia.ui_lists.details.cases.ListDetailsTranslationsCase
import xyz.stignarnia.ui_lists.details.recycler.ListDetailsItem
import xyz.stignarnia.ui_model.CustomList
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortType
import xyz.stignarnia.ui_model.Tip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ListDetailsViewModel @Inject constructor(
  private val mainCase: ListDetailsMainCase,
  private val itemsCase: ListDetailsItemsCase,
  private val translationsCase: ListDetailsTranslationsCase,
  private val sortCase: ListDetailsSortCase,
  private val tipsCase: ListDetailsTipsCase,
  private val showImagesProvider: ShowImagesProvider,
  private val movieImagesProvider: MovieImagesProvider,
) : ViewModel(),
  ChannelsDelegate by DefaultChannelsDelegate() {

  private val listDetailsState = MutableStateFlow<CustomList?>(null)
  private val listItemsState = MutableStateFlow<List<ListDetailsItem>?>(null)
  private val listDeleteState = MutableStateFlow<Event<Boolean>?>(null)
  private val manageModeState = MutableStateFlow(false)
  private val quickRemoveState = MutableStateFlow(false)
  private val scrollState = MutableStateFlow<Event<Boolean>?>(null)
  private val loadingState = MutableStateFlow(false)
  private val filtersVisibleState = MutableStateFlow(false)
  private val viewModeState = MutableStateFlow(ListViewMode.LIST_NORMAL)

  fun loadDetails(id: Long) {
    viewModelScope.launch {
      val list = mainCase.loadDetails(id)
      val (listItems, totalCount) = itemsCase.loadItems(list)

      listDetailsState.value = list
      listItemsState.value = listItems
      manageModeState.value = false
      filtersVisibleState.value = totalCount > 0
      quickRemoveState.value = mainCase.isQuickRemoveEnabled(list)

      val tip = Tip.LIST_ITEM_SWIPE_DELETE
      if (listItems.isNotEmpty() && !tipsCase.isTipShown(tip)) {
        messageChannel.send(MessageEvent.Info(tip.textResId, isIndefinite = true))
        tipsCase.setTipShown(tip)
      }
    }
  }

  fun loadMissingImage(
    item: ListDetailsItem,
    force: Boolean,
  ) {
    viewModelScope.launch {
      updateItem(item.copy(isLoading = true))
      try {
        val image =
          when {
            item.isShow() -> showImagesProvider.loadRemoteImage(item.requireShow(), item.image.type, force)
            item.isMovie() -> movieImagesProvider.loadRemoteImage(item.requireMovie(), item.image.type, force)
            else -> throw IllegalStateException()
          }
        updateItem(item.copy(isLoading = false, image = image))
      } catch (t: Throwable) {
        updateItem(item.copy(isLoading = false, image = Image.createUnavailable(item.image.type)))
      }
    }
  }

  fun loadMissingTranslation(item: ListDetailsItem) {
    if (item.translation != null || translationsCase.getLanguage() == Config.DEFAULT_LANGUAGE) return
    viewModelScope.launch {
      try {
        val translation = translationsCase.loadTranslation(item, false)
        updateItem(item.copy(translation = translation))
      } catch (error: Throwable) {
        Timber.e(error)
      }
    }
  }

  fun setReorderMode(
    listId: Long,
    isReorderMode: Boolean,
  ) {
    viewModelScope.launch {
      if (isReorderMode) {
        val list = mainCase.loadDetails(listId).copy(
          sortByLocal = SortOrder.RANK,
          sortHowLocal = SortType.ASCENDING,
          filterTypeLocal = Mode.getAll(),
        )
        val listItems = itemsCase.loadItems(list).first.map { it.copy(isManageMode = true) }
        listItemsState.value = listItems
        manageModeState.value = true
        filtersVisibleState.value = false
        scrollState.value = Event(false)
      } else {
        val list = mainCase.loadDetails(listId)
        val listItems = itemsCase.loadItems(list).first.map { it.copy(isManageMode = false) }
        listItemsState.value = listItems
        manageModeState.value = false
        filtersVisibleState.value = true
        scrollState.value = Event(true)
      }
    }
  }

  fun updateRanks(
    listId: Long,
    items: List<ListDetailsItem>,
  ) {
    viewModelScope.launch {
      val updatedItems = mainCase.updateRanks(listId, items)
      listItemsState.value = updatedItems
    }
  }

  fun setSortOrder(
    id: Long,
    sortOrder: SortOrder,
    sortType: SortType,
  ) {
    viewModelScope.launch {
      val list = sortCase.setSortOrder(id, sortOrder, sortType)

      val currentItems = uiState.value.listItems?.toList() ?: emptyList()
      val sortedItems = itemsCase.sortItems(
        currentItems,
        list.sortByLocal,
        list.sortHowLocal,
        list.filterTypeLocal,
      )

      listDetailsState.value = list
      listItemsState.value = sortedItems
      scrollState.value = Event(true)
    }
  }

  fun setFilterTypes(
    listId: Long,
    types: List<Mode>,
  ) {
    viewModelScope.launch {
      val list = sortCase.setFilterTypes(listId, types)
      val (sortedItems, _) = itemsCase.loadItems(list)

      listDetailsState.value = list
      listItemsState.value = sortedItems
      filtersVisibleState.value = true
      scrollState.value = Event(true)
    }
  }

  fun deleteList(listId: Long) {
    viewModelScope.launch {
      try {
        mainCase.deleteList(listId)
        loadingState.value = false
        listDeleteState.value = Event(true)
      } catch (error: Throwable) {
        loadingState.value = false
        messageChannel.send(MessageEvent.Error(R.string.errorCouldNotDeleteList))
      }
    }
  }

  fun deleteListItem(
    listId: Long,
    item: ListDetailsItem,
  ) {
    viewModelScope.launch {
      val type =
        when {
          item.isShow() -> SHOWS
          item.isMovie() -> MOVIES
          else -> throw IllegalStateException()
        }
      itemsCase.deleteListItem(listId, item.getTmdbId(), type)
      loadDetails(listId)
    }
  }

  private fun updateItem(newItem: ListDetailsItem) {
    listItemsState.update { state ->
      state?.toMutableList()?.apply {
        findReplace(newItem) { it.id == newItem.id }
      }
    }
  }

  val uiState = combine(
    listDetailsState,
    listItemsState,
    manageModeState,
    quickRemoveState,
    loadingState,
    listDeleteState,
    scrollState,
    filtersVisibleState,
    viewModeState,
  ) { s1, s2, s3, s4, s5, s6, s7, s8, s9 ->
    ListDetailsUiState(
      listDetails = s1,
      listItems = s2,
      isManageMode = s3,
      isQuickRemoveEnabled = s4,
      isLoading = s5,
      deleteEvent = s6,
      resetScroll = s7,
      isFiltersVisible = s8,
      viewMode = s9,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
    initialValue = ListDetailsUiState(),
  )
}
