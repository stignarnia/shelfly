package xyz.stignarnia.uiBase.common.sheets.contextMenu.movie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiBase.R
import xyz.stignarnia.uiBase.common.sheets.contextMenu.events.FinishUiEvent
import xyz.stignarnia.uiBase.common.sheets.contextMenu.events.SelectDateUiEvent
import xyz.stignarnia.uiBase.common.sheets.contextMenu.movie.cases.MovieContextMenuHiddenCase
import xyz.stignarnia.uiBase.common.sheets.contextMenu.movie.cases.MovieContextMenuLoadItemCase
import xyz.stignarnia.uiBase.common.sheets.contextMenu.movie.cases.MovieContextMenuMyMoviesCase
import xyz.stignarnia.uiBase.common.sheets.contextMenu.movie.cases.MovieContextMenuPinnedCase
import xyz.stignarnia.uiBase.common.sheets.contextMenu.movie.cases.MovieContextMenuWatchlistCase
import xyz.stignarnia.uiBase.common.sheets.contextMenu.movie.helpers.MovieContextItem
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiBase.utilities.events.MessageEvent
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.uiBase.utilities.extensions.rethrowCancellation
import xyz.stignarnia.uiBase.viewmodel.ChannelsDelegate
import xyz.stignarnia.uiBase.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.ProgressDateSelectionType.ALWAYS_ASK
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlin.properties.Delegates.notNull

@HiltViewModel
class MovieContextMenuViewModel
  @Inject
  constructor(
    private val loadItemCase: MovieContextMenuLoadItemCase,
    private val myMoviesCase: MovieContextMenuMyMoviesCase,
    private val watchlistCase: MovieContextMenuWatchlistCase,
    private val hiddenCase: MovieContextMenuHiddenCase,
    private val pinnedCase: MovieContextMenuPinnedCase,
    private val settingsRepository: SettingsRepository,
  ) : ViewModel(),
    ChannelsDelegate by DefaultChannelsDelegate() {
    private var movieId by notNull<IdTmdb>()

    private val loadingState = MutableStateFlow(false)
    private val itemState = MutableStateFlow<MovieContextItem?>(null)

    fun loadMovie(idTmdb: IdTmdb) {
      viewModelScope.launch {
        movieId = idTmdb

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

    fun moveToMyMovies(
      isCustomDateSelected: Boolean = false,
      customDate: ZonedDateTime? = null,
    ) {
      viewModelScope.launch {
        try {
          val movie = itemState.value?.movie
          val isCustomDateAlwaysAsk = { settingsRepository.progressDateSelectionType == ALWAYS_ASK }
          if (movie != null && !isCustomDateSelected && isCustomDateAlwaysAsk()) {
            eventChannel.send(Event(SelectDateUiEvent(movie)))
            return@launch
          }
          myMoviesCase.moveToMyMovies(movieId, customDate)
          eventChannel.send(Event(FinishUiEvent(true)))
        } catch (error: Throwable) {
          onError(error)
        }
      }
    }

    fun removeFromMyMovies() {
      viewModelScope.launch {
        try {
          myMoviesCase.removeFromMyMovies(movieId)
          eventChannel.send(Event(FinishUiEvent(true)))
        } catch (error: Throwable) {
          onError(error)
        }
      }
    }

    fun moveToWatchlist() {
      viewModelScope.launch {
        try {
          watchlistCase.moveToWatchlist(movieId)
          eventChannel.send(Event(FinishUiEvent(true)))
        } catch (error: Throwable) {
          onError(error)
        }
      }
    }

    fun removeFromWatchlist() {
      viewModelScope.launch {
        try {
          watchlistCase.removeFromWatchlist(movieId)
          eventChannel.send(Event(FinishUiEvent(true)))
        } catch (error: Throwable) {
          onError(error)
        }
      }
    }

    fun moveToHidden() {
      viewModelScope.launch {
        try {
          hiddenCase.moveToHidden(movieId)
          eventChannel.send(Event(FinishUiEvent(true)))
        } catch (error: Throwable) {
          onError(error)
        }
      }
    }

    fun removeFromHidden() {
      viewModelScope.launch {
        try {
          hiddenCase.removeFromHidden(movieId)
          eventChannel.send(Event(FinishUiEvent(true)))
        } catch (error: Throwable) {
          onError(error)
        }
      }
    }

    fun addToTopPinned() {
      viewModelScope.launch {
        pinnedCase.addToTopPinned(movieId)
        eventChannel.send(Event(FinishUiEvent(true)))
      }
    }

    fun removeFromTopPinned() {
      viewModelScope.launch {
        pinnedCase.removeFromTopPinned(movieId)
        eventChannel.send(Event(FinishUiEvent(true)))
      }
    }

    private suspend fun onError(error: Throwable) {
      loadingState.value = false
      messageChannel.send(MessageEvent.Error(R.string.errorGeneral))
      rethrowCancellation(error)
    }

    val uiState =
      combine(
        loadingState,
        itemState,
      ) { s1, s2 ->
        MovieContextMenuUiState(
          isLoading = s1,
          item = s2,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
        initialValue = MovieContextMenuUiState(),
      )
  }
