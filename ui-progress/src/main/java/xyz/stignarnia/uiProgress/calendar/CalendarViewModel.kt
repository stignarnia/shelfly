package xyz.stignarnia.uiProgress.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.stignarnia.common.Config
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.uiBase.utilities.extensions.findReplace
import xyz.stignarnia.uiBase.viewmodel.ChannelsDelegate
import xyz.stignarnia.uiBase.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.uiModel.CalendarMode
import xyz.stignarnia.uiModel.EpisodeBundle
import xyz.stignarnia.uiModel.IdTmdb
import xyz.stignarnia.uiModel.Image
import xyz.stignarnia.uiProgress.calendar.cases.items.CalendarFutureCase
import xyz.stignarnia.uiProgress.calendar.cases.items.CalendarRecentsCase
import xyz.stignarnia.uiProgress.calendar.recycler.CalendarListItem
import xyz.stignarnia.uiProgress.main.EpisodeCheckActionUiEvent
import xyz.stignarnia.uiProgress.main.ProgressMainUiState
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel
  @Inject
  constructor(
    private val recentsCase: CalendarRecentsCase,
    private val futureCase: CalendarFutureCase,
    private val imagesProvider: ShowImagesProvider,
    private val translationsRepository: TranslationsRepository,
    private val settingsRepository: SettingsRepository,
  ) : ViewModel(),
    ChannelsDelegate by DefaultChannelsDelegate() {
    private var loadItemsJob: Job? = null
    private var loadTranslationJobs: MutableSet<IdTmdb> = mutableSetOf()

    private val itemsState = MutableStateFlow<List<CalendarListItem>?>(null)
    private val modeState = MutableStateFlow(CalendarMode.PRESENT_FUTURE)

    private var mode = CalendarMode.PRESENT_FUTURE
    private var searchQuery: String? = null
    private var timestamp = 0L

    fun handleParentAction(state: ProgressMainUiState) {
      when {
        this.timestamp != state.timestamp && state.timestamp != 0L -> {
          this.timestamp = state.timestamp ?: 0L
          loadItems()
        }

        this.mode != state.calendarMode -> {
          this.mode = state.calendarMode ?: CalendarMode.PRESENT_FUTURE
          loadItems()
        }

        this.searchQuery != state.searchQuery -> {
          this.searchQuery = state.searchQuery
          loadItems()
        }
      }
    }

    private fun loadItems() {
      loadItemsJob?.cancel()
      loadItemsJob =
        viewModelScope.launch {
          val items =
            when (mode) {
              CalendarMode.PRESENT_FUTURE -> futureCase.loadItems(searchQuery)
              CalendarMode.RECENTS -> recentsCase.loadItems(searchQuery)
            }
          itemsState.value = items
          modeState.value = mode
        }
    }

    fun togglePremieresFilter() {
      val isPremieresEnabled = settingsRepository.filters.calendarPremieresOnly
      settingsRepository.filters.calendarPremieresOnly = !isPremieresEnabled
      loadItems()
    }

    fun onEpisodeChecked(episode: CalendarListItem.Episode) {
      viewModelScope.launch {
        val bundle = EpisodeBundle(episode.episode, episode.season, episode.show)
        eventChannel.send(
          EpisodeCheckActionUiEvent(
            episode = bundle,
            dateSelectionType = settingsRepository.progressDateSelectionType,
          ),
        )
      }
    }

    fun findMissingImage(
      item: CalendarListItem,
      force: Boolean,
    ) {
      check(item is CalendarListItem.Episode)
      viewModelScope.launch {
        updateItem(item.copy(isLoading = true))
        try {
          val image = imagesProvider.loadRemoteImage(item.show, item.image.type, force)
          updateItem(item.copy(image = image, isLoading = false))
        } catch (t: Throwable) {
          val unavailable = Image.createUnavailable(item.image.type)
          updateItem(item.copy(image = unavailable, isLoading = false))
        }
      }
    }

    fun findMissingTranslation(item: CalendarListItem) {
      check(item is CalendarListItem.Episode)
      val showId = item.show.ids.tmdb
      val language = translationsRepository.getLanguage()
      if (item.translations?.show != null ||
        language == Config.DEFAULT_LANGUAGE ||
        loadTranslationJobs.contains(showId)
      ) {
        return
      }
      viewModelScope.launch {
        try {
          val translation = translationsRepository.loadTranslation(item.show, language)
          val translations = item.translations?.copy(show = translation)
          updateItem(item.copy(translations = translations))
        } catch (error: Throwable) {
          Timber.e(error)
        } finally {
          loadTranslationJobs.remove(showId)
        }
      }
      loadTranslationJobs.add(showId)
    }

    private fun updateItem(newItem: CalendarListItem.Episode) {
      itemsState.update { value ->
        value?.toMutableList()?.apply {
          findReplace(newItem) { it.isSameAs(newItem) }
        }
      }
      modeState.update { mode }
    }

    val uiState =
      combine(
        itemsState,
        modeState,
      ) { s1, s2 ->
        CalendarUiState(
          items = s1,
          mode = s2,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
        initialValue = CalendarUiState(),
      )
  }
