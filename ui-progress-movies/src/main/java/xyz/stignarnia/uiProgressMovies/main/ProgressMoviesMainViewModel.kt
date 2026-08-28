package xyz.stignarnia.uiProgressMovies.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.stignarnia.uiBase.events.Event
import xyz.stignarnia.uiBase.events.EventsManager
import xyz.stignarnia.uiBase.events.ReloadData
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.uiModel.CalendarMode
import xyz.stignarnia.uiModel.Movie
import xyz.stignarnia.uiProgressMovies.main.cases.ProgressMoviesMainCase
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class ProgressMoviesMainViewModel
  @Inject
  constructor(
    private val moviesCase: ProgressMoviesMainCase,
    private val eventsManager: EventsManager,
  ) : ViewModel() {
    private val timestampState = MutableStateFlow<Long?>(null)
    private val searchQueryState = MutableStateFlow<String?>(null)
    private val calendarModeState = MutableStateFlow<CalendarMode?>(null)

    private var calendarMode = CalendarMode.PRESENT_FUTURE

    init {
      viewModelScope.launch {
        eventsManager.events.collect { onEvent(it) }
      }
    }

    fun loadProgress() {
      viewModelScope.launch {
        timestampState.value = System.currentTimeMillis()
        calendarModeState.value = calendarMode
      }
    }

    fun onSearchQuery(searchQuery: String) {
      searchQueryState.value = searchQuery
    }

    fun toggleCalendarMode() {
      calendarMode =
        when (calendarMode) {
          CalendarMode.PRESENT_FUTURE -> CalendarMode.RECENTS
          CalendarMode.RECENTS -> CalendarMode.PRESENT_FUTURE
        }
      calendarModeState.value = calendarMode
    }

    fun setWatchedMovie(
      movie: Movie,
      customDate: ZonedDateTime? = null,
    ) {
      viewModelScope.launch {
        moviesCase.addToMyMovies(movie, customDate)
        timestampState.value = System.currentTimeMillis()
      }
    }

    private fun onEvent(event: Event) {
      if (event is ReloadData) {
        loadProgress()
      }
    }

    val uiState =
      combine(
        timestampState,
        searchQueryState,
        calendarModeState,
      ) { s1, s2, s3 ->
        ProgressMoviesMainUiState(
          timestamp = s1,
          searchQuery = s2,
          calendarMode = s3,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
        initialValue = ProgressMoviesMainUiState(),
      )
  }
