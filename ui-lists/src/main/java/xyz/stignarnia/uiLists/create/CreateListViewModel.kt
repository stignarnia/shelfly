package xyz.stignarnia.uiLists.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.stignarnia.common.errors.ErrorHelper
import xyz.stignarnia.common.errors.ShelflyError.AccountLimitsError
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiBase.utilities.events.MessageEvent
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.uiBase.viewmodel.ChannelsDelegate
import xyz.stignarnia.uiBase.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.uiLists.R
import xyz.stignarnia.uiLists.create.cases.CreateListCase
import xyz.stignarnia.uiLists.create.cases.ListDetailsCase
import xyz.stignarnia.uiModel.CustomList
import javax.inject.Inject

@HiltViewModel
class CreateListViewModel
  @Inject
  constructor(
    private val createListCase: CreateListCase,
    private val listDetailsCase: ListDetailsCase,
  ) : ViewModel(),
    ChannelsDelegate by DefaultChannelsDelegate() {
    private val detailsState = MutableStateFlow<CustomList?>(null)
    private val loadingState = MutableStateFlow(false)
    private val listUpdateState = MutableStateFlow<Event<CustomList>?>(null)

    fun loadDetails(id: Long) {
      viewModelScope.launch {
        loadingState.value = true
        detailsState.value = listDetailsCase.loadDetails(id)
        loadingState.value = false
      }
    }

    fun createList(
      name: String,
      description: String?,
    ) {
      if (name.trim().isBlank()) return
      viewModelScope.launch {
        try {
          loadingState.value = true
          val list = createListCase.createList(name, description)
          listUpdateState.value = Event(list)
        } catch (error: Throwable) {
          loadingState.value = false
          handleError(error, R.string.errorCouldNotCreateList)
        }
      }
    }

    fun updateList(list: CustomList) {
      if (list.name.trim().isBlank()) return
      viewModelScope.launch {
        try {
          loadingState.value = true
          detailsState.value = list
          val updatedList = createListCase.updateList(list)
          listUpdateState.value = Event(updatedList)
        } catch (error: Throwable) {
          detailsState.value = list
          loadingState.value = false
          handleError(error, R.string.errorCouldNotUpdateList)
        }
      }
    }

    private suspend fun handleError(
      error: Throwable,
      defaultErrorMessage: Int,
    ) {
      when (ErrorHelper.parse(error)) {
        AccountLimitsError -> {
          messageChannel.send(MessageEvent.Error(R.string.errorAccountListsLimitsReached))
        }

        else -> {
          messageChannel.send(MessageEvent.Error(defaultErrorMessage))
        }
      }
    }

    val uiState =
      combine(
        detailsState,
        loadingState,
        listUpdateState,
      ) { s1, s2, s3 ->
        CreateListUiState(
          listDetails = s1,
          isLoading = s2,
          onListUpdated = s3,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
        initialValue = CreateListUiState(),
      )
  }
