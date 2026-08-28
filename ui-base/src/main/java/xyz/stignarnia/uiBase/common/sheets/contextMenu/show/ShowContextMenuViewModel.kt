package xyz.stignarnia.uiBase.common.sheets.contextMenu.show

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiBase.R
import xyz.stignarnia.uiBase.common.sheets.contextMenu.events.FinishUiEvent
import xyz.stignarnia.uiBase.common.sheets.contextMenu.show.cases.ShowContextMenuHiddenCase
import xyz.stignarnia.uiBase.common.sheets.contextMenu.show.cases.ShowContextMenuLoadItemCase
import xyz.stignarnia.uiBase.common.sheets.contextMenu.show.cases.ShowContextMenuMyShowsCase
import xyz.stignarnia.uiBase.common.sheets.contextMenu.show.cases.ShowContextMenuOnHoldCase
import xyz.stignarnia.uiBase.common.sheets.contextMenu.show.cases.ShowContextMenuPinnedCase
import xyz.stignarnia.uiBase.common.sheets.contextMenu.show.cases.ShowContextMenuWatchlistCase
import xyz.stignarnia.uiBase.common.sheets.contextMenu.show.helpers.ShowContextItem
import xyz.stignarnia.uiBase.network.NetworkStatusProvider
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiBase.utilities.events.MessageEvent
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.uiBase.utilities.extensions.launchDelayed
import xyz.stignarnia.uiBase.utilities.extensions.rethrowCancellation
import xyz.stignarnia.uiBase.viewmodel.ChannelsDelegate
import xyz.stignarnia.uiBase.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.ImageType
import javax.inject.Inject
import kotlin.properties.Delegates.notNull

@HiltViewModel
class ShowContextMenuViewModel
  @Inject
  constructor(
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
        val progressJob =
          launchDelayed(250) {
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
          val result =
            watchlistCase.moveToWatchlist(
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
          val result =
            hiddenCase.moveToHidden(
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

    val uiState =
      combine(
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
