package xyz.stignarnia.ui_base.common.sheets.context_menu.show

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_base.R
import xyz.stignarnia.ui_base.common.sheets.context_menu.events.FinishUiEvent
import xyz.stignarnia.ui_base.common.sheets.context_menu.show.cases.ShowContextMenuHiddenCase
import xyz.stignarnia.ui_base.common.sheets.context_menu.show.cases.ShowContextMenuLoadItemCase
import xyz.stignarnia.ui_base.common.sheets.context_menu.show.cases.ShowContextMenuMyShowsCase
import xyz.stignarnia.ui_base.common.sheets.context_menu.show.cases.ShowContextMenuOnHoldCase
import xyz.stignarnia.ui_base.common.sheets.context_menu.show.cases.ShowContextMenuPinnedCase
import xyz.stignarnia.ui_base.common.sheets.context_menu.show.cases.ShowContextMenuWatchlistCase
import xyz.stignarnia.ui_base.common.sheets.context_menu.show.helpers.ShowContextItem
import xyz.stignarnia.ui_base.network.NetworkStatusProvider
import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_base.utilities.events.MessageEvent
import xyz.stignarnia.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.ui_base.utilities.extensions.launchDelayed
import xyz.stignarnia.ui_base.utilities.extensions.rethrowCancellation
import xyz.stignarnia.ui_base.viewmodel.ChannelsDelegate
import xyz.stignarnia.ui_base.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.ui_model.IdTmdb
import xyz.stignarnia.ui_model.ImageType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates.notNull

@HiltViewModel
class ShowContextMenuViewModel @Inject constructor(
  private val loadItemCase: ShowContextMenuLoadItemCase,
  private val myShowsCase: ShowContextMenuMyShowsCase,
  private val watchlistCase: ShowContextMenuWatchlistCase,
  private val hiddenCase: ShowContextMenuHiddenCase,
  private val pinnedCase: ShowContextMenuPinnedCase,
  private val onHoldCase: ShowContextMenuOnHoldCase,
  private val imagesProvider: ShowImagesProvider,
  private val networkProvider: NetworkStatusProvider,
  private val settingsRepository: SettingsRepository,
) : ViewModel(),
  ChannelsDelegate by DefaultChannelsDelegate() {

  private var showId by notNull<IdTmdb>()

  private val loadingState = MutableStateFlow(false)
  private val loadingSecondaryState = MutableStateFlow(false)
  private val itemState = MutableStateFlow<ShowContextItem?>(null)

  fun loadShow(idTmdb: IdTmdb) {
    viewModelScope.launch {
      showId = idTmdb

      try {
        loadingState.value = true
        val item = loadItemCase.loadItem(idTmdb)
        itemState.value = item
      } catch (error: Throwable) {
        messageChannel.send(MessageEvent.Error(R.string.errorGeneral))
      } finally {
        loadingState.value = false
      }
    }
  }

  fun moveToMyShows() {
    viewModelScope.launch {
      if (!networkProvider.isOnline()) {
        messageChannel.send(MessageEvent.Error(R.string.errorNoInternetConnection))
        return@launch
      }
      val progressJob = launchDelayed(250) {
        loadingSecondaryState.value = true
      }
      try {
        val result = myShowsCase.moveToMyShows(showId)
        preloadImage()
        eventChannel.send(Event(FinishUiEvent(true)))
      } catch (error: Throwable) {
        onError(error)
      } finally {
        progressJob.cancel()
      }
    }
  }

  fun removeFromMyShows() {
    viewModelScope.launch {
      try {
        myShowsCase.removeFromMyShows(
          tmdbId = showId,
          removeLocalData = networkProvider.isOnline(),
        )
        eventChannel.send(Event(FinishUiEvent(true)))
      } catch (error: Throwable) {
        onError(error)
      }
    }
  }

  fun moveToWatchlist() {
    viewModelScope.launch {
      try {
        val result = watchlistCase.moveToWatchlist(
          tmdbId = showId,
          removeLocalData = networkProvider.isOnline(),
        )
        eventChannel.send(Event(FinishUiEvent(true)))
      } catch (error: Throwable) {
        onError(error)
      }
    }
  }

  fun removeFromWatchlist() {
    viewModelScope.launch {
      try {
        watchlistCase.removeFromWatchlist(showId)
        eventChannel.send(Event(FinishUiEvent(true)))
      } catch (error: Throwable) {
        onError(error)
      }
    }
  }

  fun moveToHidden() {
    viewModelScope.launch {
      try {
        val result = hiddenCase.moveToHidden(
          tmdbId = showId,
          removeLocalData = networkProvider.isOnline(),
        )
        eventChannel.send(Event(FinishUiEvent(true)))
      } catch (error: Throwable) {
        onError(error)
      }
    }
  }

  fun removeFromHidden() {
    viewModelScope.launch {
      try {
        hiddenCase.removeFromHidden(showId)
        eventChannel.send(Event(FinishUiEvent(true)))
      } catch (error: Throwable) {
        onError(error)
      }
    }
  }

  fun addToTopPinned() {
    viewModelScope.launch {
      pinnedCase.addToTopPinned(showId)
      eventChannel.send(Event(FinishUiEvent(true)))
    }
  }

  fun removeFromTopPinned() {
    viewModelScope.launch {
      pinnedCase.removeFromTopPinned(showId)
      eventChannel.send(Event(FinishUiEvent(true)))
    }
  }

  fun addToOnHoldPinned() {
    viewModelScope.launch {
      onHoldCase.addToOnHold(showId)
      eventChannel.send(Event(FinishUiEvent(true)))
    }
  }

  fun removeFromOnHoldPinned() {
    viewModelScope.launch {
      onHoldCase.removeFromOnHold(showId)
      eventChannel.send(Event(FinishUiEvent(true)))
    }
  }

  private suspend fun preloadImage() {
    try {
      val show = itemState.value?.show
      show?.let {
        imagesProvider.loadRemoteImage(it, ImageType.FANART)
      }
    } catch (error: Throwable) {
      Timber.e(error)
      rethrowCancellation(error)
    }
  }

  private suspend fun onError(error: Throwable) {
    loadingState.value = false
    loadingSecondaryState.value = false
    messageChannel.send(MessageEvent.Error(R.string.errorGeneral))
    rethrowCancellation(error)
  }

  val uiState = combine(
    loadingState,
    loadingSecondaryState,
    itemState,
  ) { s1, s2, s3 ->
    ShowContextMenuUiState(
      isLoading = s1,
      isLoadingSecondary = s2,
      item = s3,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
    initialValue = ShowContextMenuUiState(),
  )
}
