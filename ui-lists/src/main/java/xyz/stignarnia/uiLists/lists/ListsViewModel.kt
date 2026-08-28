package xyz.stignarnia.uiLists.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.stignarnia.repository.images.MovieImagesProvider
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.uiBase.events.EventsManager
import xyz.stignarnia.uiBase.events.ReloadData
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.uiBase.utilities.extensions.findReplace
import xyz.stignarnia.uiLists.lists.cases.MainListsCase
import xyz.stignarnia.uiLists.lists.cases.SortOrderListsCase
import xyz.stignarnia.uiLists.lists.helpers.ListsItemImage
import xyz.stignarnia.uiLists.lists.recycler.ListsItem
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import javax.inject.Inject
import xyz.stignarnia.uiBase.events.Event as EventSync

@HiltViewModel
class ListsViewModel
  @Inject
  constructor(
    private val mainCase: MainListsCase,
    private val sortCase: SortOrderListsCase,
    private val showImagesProvider: ShowImagesProvider,
    private val movieImagesProvider: MovieImagesProvider,
    private val eventsManager: EventsManager,
  ) : ViewModel() {
    private var loadItemsJob: Job? = null

    private val itemsState = MutableStateFlow<List<ListsItem>?>(null)
    private val scrollState = MutableStateFlow(Event(false))
    private val sortOrderState = MutableStateFlow<Pair<SortOrder, SortType>?>(null)

    init {
      viewModelScope.launch {
        eventsManager.events.collect { onEvent(it) }
      }
    }

    fun loadItems(
      resetScroll: Boolean,
      searchQuery: String? = null,
    ) {
      loadItemsJob?.cancel()
      loadItemsJob =
        viewModelScope.launch {
          sortOrderState.value = sortCase.loadSortOrder()
          itemsState.value = mainCase.loadLists(searchQuery)
          scrollState.value = Event(resetScroll)
        }
    }

    fun setSortOrder(
      sortOrder: SortOrder,
      sortType: SortType,
    ) {
      viewModelScope.launch {
        sortCase.setSortOrder(sortOrder, sortType)
        loadItems(resetScroll = true)
      }
    }

    fun loadMissingImage(
      item: ListsItem,
      itemImage: ListsItemImage,
      force: Boolean,
    ) {
      viewModelScope.launch {
        try {
          val imageType = itemImage.image.type

          val image =
            when {
              itemImage.isShow() -> showImagesProvider.loadRemoteImage(itemImage.show!!, imageType, force)
              itemImage.isMovie() -> movieImagesProvider.loadRemoteImage(itemImage.movie!!, imageType, force)
              else -> throw IllegalStateException()
            }

          val updateItemImage = itemImage.copy(image = image)
          val updateImages = item.images.toMutableList()
          updateImages.findReplace(updateItemImage) { it.getIds()?.tmdb == updateItemImage.getIds()?.tmdb }
          updateItem(item.copy(images = updateImages))
        } catch (t: Throwable) {
          val updateItemImage = itemImage.copy(image = Image.createUnavailable(itemImage.image.type))
          val updateImages = item.images.toMutableList()
          updateImages.findReplace(updateItemImage) { it.getIds()?.tmdb == updateItemImage.getIds()?.tmdb }
          updateItem(item.copy(images = updateImages))
        }
      }
    }

    private fun updateItem(newItem: ListsItem) {
      val currentItems = uiState.value.items?.toMutableList() ?: mutableListOf()
      currentItems.findReplace(newItem) { it.list.id == newItem.list.id }
      itemsState.value = currentItems
    }

    private fun onEvent(event: EventSync) {
      if (event is ReloadData) {
        loadItems(resetScroll = true)
      }
    }

    val uiState =
      combine(
        itemsState,
        scrollState,
        sortOrderState,
      ) { s1, s2, s3 ->
        ListsUiState(
          items = s1,
          resetScroll = s2,
          sortOrder = s3,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
        initialValue = ListsUiState(),
      )
  }
