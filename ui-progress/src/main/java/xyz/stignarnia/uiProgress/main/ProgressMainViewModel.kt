package xyz.stignarnia.uiProgress.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.stignarnia.uiBase.events.EventsManager
import xyz.stignarnia.uiBase.events.ReloadData
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiBase.utilities.events.MessageEvent
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.uiBase.viewmodel.ChannelsDelegate
import xyz.stignarnia.uiBase.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.uiModel.CalendarMode
import xyz.stignarnia.uiModel.Episode
import xyz.stignarnia.uiModel.EpisodeBundle
import xyz.stignarnia.uiModel.Show
import xyz.stignarnia.uiProgress.R
import xyz.stignarnia.uiProgress.main.cases.ProgressMainEpisodesCase
import java.time.ZonedDateTime
import javax.inject.Inject
import xyz.stignarnia.uiBase.events.Event as EventSync

@HiltViewModel
class ProgressMainViewModel
  @Inject
  constructor(
    private val episodesCase: ProgressMainEpisodesCase,
    private val eventsManager: EventsManager,
  ) : ViewModel(),
    ChannelsDelegate by DefaultChannelsDelegate() {
    private val timestampState = MutableStateFlow<Long?>(null)
    private val searchQueryState = MutableStateFlow<String?>(null)
    private val calendarModeState = MutableStateFlow<CalendarMode?>(null)
    private val scrollState = MutableStateFlow<Event<Boolean>?>(null)

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

    fun onSearchQuery(searchQuery: String?) {
      searchQueryState.value = searchQuery ?: ""
    }

    fun onEpisodeDetails(
      show: Show,
      episode: Episode,
    ) {
      viewModelScope.launch {
        val isWatched = episodesCase.isWatched(show, episode)
        eventChannel.send(
          OpenEpisodeDetails(
            show = show,
            episode = episode,
            isWatched = isWatched,
          ),
        )
      }
    }

    fun toggleCalendarMode() {
      calendarMode =
        when (calendarMode) {
          CalendarMode.PRESENT_FUTURE -> CalendarMode.RECENTS
          CalendarMode.RECENTS -> CalendarMode.PRESENT_FUTURE
        }
      calendarModeState.value = calendarMode
    }

    fun setWatchedEpisode(
      bundle: EpisodeBundle,
      customDate: ZonedDateTime? = null,
    ) {
      viewModelScope.launch {
        if (!bundle.episode.hasAired(bundle.season)) {
          messageChannel.send(MessageEvent.Info(R.string.errorEpisodeNotAired))
          return@launch
        }
        episodesCase.setEpisodeWatched(bundle, customDate)
        timestampState.value = System.currentTimeMillis()
        scrollState.value = Event(false)
      }
    }

    private fun onEvent(event: EventSync) {
      if (event is ReloadData) {
        loadProgress()
      }
    }

    val uiState =
      combine(
        timestampState,
        searchQueryState,
        calendarModeState,
        scrollState,
      ) { s1, s2, s3, s4 ->
        ProgressMainUiState(
          timestamp = s1,
          searchQuery = s2,
          calendarMode = s3,
          resetScroll = s4,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
        initialValue = ProgressMainUiState(),
      )
  }
