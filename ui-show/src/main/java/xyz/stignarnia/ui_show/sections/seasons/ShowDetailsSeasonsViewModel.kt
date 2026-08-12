package xyz.stignarnia.ui_show.sections.seasons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_base.utilities.events.MessageEvent
import xyz.stignarnia.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.ui_base.viewmodel.ChannelsDelegate
import xyz.stignarnia.ui_base.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.ui_model.ProgressDateSelectionType.ALWAYS_ASK
import xyz.stignarnia.ui_model.Season
import xyz.stignarnia.ui_model.Show
import xyz.stignarnia.ui_show.R
import xyz.stignarnia.ui_show.ShowDetailsEvent
import xyz.stignarnia.ui_show.episodes.cases.EpisodesMarkWatchedCase
import xyz.stignarnia.ui_show.quicksetup.QuickSetupListItem
import xyz.stignarnia.ui_show.sections.seasons.ShowDetailsSeasonsEvent.RequestWidgetsUpdate
import xyz.stignarnia.ui_show.sections.seasons.cases.ShowDetailsLoadSeasonsCase
import xyz.stignarnia.ui_show.sections.seasons.cases.ShowDetailsQuickProgressCase
import xyz.stignarnia.ui_show.sections.seasons.cases.ShowDetailsWatchedSeasonCase
import xyz.stignarnia.ui_show.sections.seasons.helpers.SeasonsCache
import xyz.stignarnia.ui_show.sections.seasons.recycler.SeasonListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class ShowDetailsSeasonsViewModel @Inject constructor(
  private val loadSeasonsCase: ShowDetailsLoadSeasonsCase,
  private val quickProgressCase: ShowDetailsQuickProgressCase,
  private val watchedSeasonCase: ShowDetailsWatchedSeasonCase,
  private val markWatchedCase: EpisodesMarkWatchedCase,
  private val seasonsCache: SeasonsCache,
  private val settingsRepository: SettingsRepository,
) : ViewModel(),
  ChannelsDelegate by DefaultChannelsDelegate() {

  private lateinit var show: Show

  private val loadingState = MutableStateFlow(true)
  private val seasonsState = MutableStateFlow<List<SeasonListItem>?>(null)

  private var areSeasonsLocal = false

  fun handleEvent(event: ShowDetailsEvent<*>) {
    when (event) {
      is ShowDetailsEvent.RefreshSeasons -> refreshSeasons()
      else -> Unit
    }
  }

  fun loadSeasons(show: Show) {
    if (this::show.isInitialized) return
    this.show = show
    viewModelScope.launch {
      try {
        val (seasons, isLocal) = loadSeasonsCase.loadSeasons(show)
        areSeasonsLocal = isLocal
        val calculated = markWatchedCase.markWatchedEpisodes(show, seasons)
        updateSeasons(calculated)
      } catch (error: Throwable) {
        updateSeasons(emptyList())
      }
    }
  }

  fun onSeasonChecked(
    season: Season,
    isChecked: Boolean,
  ) {
    if (!isChecked) {
      setSeasonWatched(season, false)
      return
    }
    if (settingsRepository.progressDateSelectionType == ALWAYS_ASK) {
      viewModelScope.launch {
        val event = ShowDetailsSeasonsEvent.OpenSeasonDateSelection(season)
        eventChannel.send(event)
      }
    } else {
      setSeasonWatched(season, true)
    }
  }

  fun setSeasonWatched(
    season: Season,
    isChecked: Boolean,
    customDate: ZonedDateTime? = null,
  ) {
    viewModelScope.launch {
      val result = watchedSeasonCase.setSeasonWatched(
        show = show,
        season = season,
        isChecked = isChecked,
        isLocal = areSeasonsLocal,
        customDate = customDate,
      )
      refreshSeasons()
    }
  }

  fun onQuickProgressSelected(quickSetupItem: QuickSetupListItem?) {
    viewModelScope.launch {
      if (quickSetupItem == null || !checkSeasonsLoaded()) {
        return@launch
      }
      if (settingsRepository.progressDateSelectionType == ALWAYS_ASK) {
        viewModelScope.launch {
          val event = ShowDetailsSeasonsEvent.OpenQuickProgressDateSelection(quickSetupItem)
          eventChannel.send(event)
        }
      } else {
        setQuickProgress(quickSetupItem, null)
      }
    }
  }

  fun setQuickProgress(
    item: QuickSetupListItem?,
    customDate: ZonedDateTime?,
  ) {
    viewModelScope.launch {
      if (item == null || !checkSeasonsLoaded()) {
        return@launch
      }

      val seasonItems = seasonsState.value?.toList() ?: emptyList()
      quickProgressCase.setQuickProgress(item, seasonItems, show, customDate)
      refreshSeasons()

      messageChannel.send(MessageEvent.Info(R.string.textShowQuickProgressDone))
    }
  }

  fun openSeasonEpisodes(season: SeasonListItem) {
    viewModelScope.launch {
      seasonsCache.setSeasons(show.ids.tmdb, seasonsState.value ?: emptyList(), areSeasonsLocal)
      val event = ShowDetailsSeasonsEvent.OpenSeasonEpisodes(show.ids.tmdb, season.season.ids.tmdb)
      eventChannel.send(event)
    }
  }

  fun refreshSeasons() {
    if (!this::show.isInitialized || seasonsState.value == null) {
      return
    }
    viewModelScope.launch {
      val seasonItems = seasonsState.value?.toList() ?: emptyList()
      val calculated = markWatchedCase.markWatchedEpisodes(show, seasonItems)
      updateSeasons(calculated)
    }
  }

  private suspend fun checkSeasonsLoaded(): Boolean {
    if (seasonsState.value == null) {
      messageChannel.send(MessageEvent.Info(R.string.errorSeasonsNotLoaded))
      return false
    }
    return true
  }

  private suspend fun updateSeasons(seasons: List<SeasonListItem>) {
    seasonsState.value = seasons
    seasonsCache.setSeasons(show.ids.tmdb, seasons, areSeasonsLocal)
    eventChannel.send(RequestWidgetsUpdate)
  }

  val uiState = combine(
    loadingState,
    seasonsState,
  ) { s1, s2 ->
    ShowDetailsSeasonsUiState(
      isLoading = s1,
      seasons = s2,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
    initialValue = ShowDetailsSeasonsUiState(),
  )
}
