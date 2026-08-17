package xyz.stignarnia.ui_progress.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import xyz.stignarnia.common.Config
import xyz.stignarnia.repository.TranslationsRepository
import xyz.stignarnia.repository.images.ShowImagesProvider
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_backup.features.export.workers.BackupExportScheduleWorker
import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.ui_base.utilities.extensions.findReplace
import xyz.stignarnia.ui_base.viewmodel.ChannelsDelegate
import xyz.stignarnia.ui_base.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.ui_model.EpisodeBundle
import xyz.stignarnia.ui_model.Image
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortType
import xyz.stignarnia.ui_progress.main.EpisodeCheckActionUiEvent
import xyz.stignarnia.ui_progress.main.ProgressMainUiState
import xyz.stignarnia.ui_progress.main.RequestWidgetsUpdate
import xyz.stignarnia.ui_progress.progress.cases.ProgressFiltersCase
import xyz.stignarnia.ui_progress.progress.cases.ProgressHeadersCase
import xyz.stignarnia.ui_progress.progress.cases.ProgressItemsCase
import xyz.stignarnia.ui_progress.progress.cases.ProgressSortOrderCase
import xyz.stignarnia.ui_progress.progress.recycler.ProgressListItem
import xyz.stignarnia.ui_model.BackupTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProgressViewModel @Inject constructor(
  private val itemsCase: ProgressItemsCase,
  private val headersCase: ProgressHeadersCase,
  private val sortOrderCase: ProgressSortOrderCase,
  private val filtersCase: ProgressFiltersCase,
  private val imagesProvider: ShowImagesProvider,
  private val workManager: WorkManager,
  private val translationsRepository: TranslationsRepository,
  private val settingsRepository: SettingsRepository,
) : ViewModel(),
  ChannelsDelegate by DefaultChannelsDelegate() {

  private var loadItemsJob: Job? = null

  private val itemsState = MutableStateFlow<List<ProgressListItem>?>(null)
  private val loadingState = MutableStateFlow(false)
  private val overscrollState = MutableStateFlow(false)
  private val scrollState = MutableStateFlow(Event(false))
  private val sortOrderState = MutableStateFlow<Event<Triple<SortOrder, SortType, Boolean>>?>(null)
  private val backupProgressState = MutableStateFlow<Int?>(null)

  private var searchQuery: String? = null
  private var timestamp = 0L
  private var hasObservedBackupRun = false

  /**
   * How far along the manual backup-and-sync run is, 0..100, or null when none
   * is running. Kept out of [uiState] because it is the state of a background
   * job rather than of this list, and it changes on its own schedule.
   */
  val backupProgress = backupProgressState.asStateFlow()

  init {
    observeBackupRun()
  }

  /**
   * Follows the run started by the pull gesture so the indicator can report it.
   *
   * WorkManager publishes progress only while the work is RUNNING and drops it
   * when the run ends, so there is no completion stage to wait for: the work
   * leaving the active set is what takes the indicator down, succeeded or not.
   */
  private fun observeBackupRun() {
    viewModelScope.launch {
      workManager
        .getWorkInfosForUniqueWorkFlow(BackupExportScheduleWorker.TAG_ONE_OFF)
        .collect { infos ->
          val active = infos.firstOrNull { !it.state.isFinished }
          when {
            active != null -> {
              hasObservedBackupRun = true
              backupProgressState.value =
                active.progress.getInt(BackupExportScheduleWorker.KEY_PROGRESS_PERCENT, 0)
            }
            // Runs from earlier launches stay on record under this name, already
            // finished. Only a run this screen watched start may take the
            // indicator down, or the reading set optimistically by the pull
            // would be cleared before WorkManager has registered the new request.
            hasObservedBackupRun -> {
              hasObservedBackupRun = false
              backupProgressState.value = null
            }
          }
        }
    }
  }

  fun onParentState(state: ProgressMainUiState) {
    when {
      this.timestamp != state.timestamp && state.timestamp != 0L -> {
        this.timestamp = state.timestamp ?: 0L
        loadItems(resetScroll = state.resetScroll?.consume() == true)
      }
      this.searchQuery != state.searchQuery -> {
        this.searchQuery = state.searchQuery
        loadItems(resetScroll = state.searchQuery.isNullOrBlank())
      }
    }
  }

  private fun loadItems(resetScroll: Boolean = false) {
    loadItemsJob?.cancel()
    loadItemsJob = viewModelScope.launch {
      loadingState.value = true

      val items = itemsCase.loadItems(searchQuery ?: "")
      itemsState.value = items
      loadingState.value = false
      scrollState.value = Event(resetScroll)
      overscrollState.value = false && items.isNotEmpty()

      eventChannel.send(RequestWidgetsUpdate)
    }
  }

  fun loadSortOrder() {
    if (itemsState.value?.isEmpty() == true) return
    viewModelScope.launch {
      val sortOrder = sortOrderCase.loadSortOrder()
      sortOrderState.value = Event(sortOrder)
    }
  }

  fun onEpisodeChecked(episode: ProgressListItem.Episode) {
    viewModelScope.launch {
      val bundle = EpisodeBundle(episode.requireEpisode(), episode.requireSeason(), episode.show)
      eventChannel.send(
        EpisodeCheckActionUiEvent(
          episode = bundle,
          dateSelectionType = settingsRepository.progressDateSelectionType,
        ),
      )
    }
  }

  fun findMissingImage(
    item: ProgressListItem,
    force: Boolean,
  ) {
    check(item is ProgressListItem.Episode)
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

  fun findMissingTranslation(item: ProgressListItem) {
    check(item is ProgressListItem.Episode)
    val language = translationsRepository.getLanguage()
    if (item.translations?.show != null || language == Config.DEFAULT_LANGUAGE) return
    viewModelScope.launch {
      try {
        val translation = translationsRepository.loadTranslation(item.show, language)
        val translations = item.translations?.copy(show = translation)
        updateItem(item.copy(translations = translations))
      } catch (error: Throwable) {
        Timber.e(error)
      }
    }
  }

  fun setSortOrder(
    sortOrder: SortOrder,
    sortType: SortType,
    newAtTop: Boolean,
  ) {
    sortOrderCase.setSortOrder(sortOrder, sortType, newAtTop)
    loadItems(resetScroll = true)
  }

  fun setUpcomingFilter(isEnabled: Boolean) {
    filtersCase.setUpcomingFilter(isEnabled)
    loadItems(resetScroll = true)
  }

  fun setOnHoldFilter(isEnabled: Boolean) {
    filtersCase.setOnHoldFilter(isEnabled)
    loadItems(resetScroll = true)
  }

  fun toggleHeaderCollapsed(headerType: ProgressListItem.Header.Type) {
    headersCase.toggleHeaderCollapsed(headerType)
    loadItems()
  }

  /**
   * Runs a backup now, in response to the pull gesture on this screen.
   *
   * Returns false when there is nothing to back up to, so the gesture can say
   * so rather than appearing to work. WorkManager keeps a run already in
   * flight, so repeated pulls do not stack up.
   */
  fun startBackupNow(): Boolean {
    if (settingsRepository.webdav.backupTarget != BackupTarget.WEBDAV) return false
    if (settingsRepository.webdav.url.isBlank()) return false
    // Set here, synchronously, so the indicator takes over from the pull in the
    // frame the gesture completes. Waiting for WorkManager to register the
    // request and report it back would blink the indicator out and in again.
    backupProgressState.value = 0
    BackupExportScheduleWorker.scheduleOneOff(workManager)
    return true
  }

  private fun updateItem(newItem: ProgressListItem) {
    itemsState.update { value ->
      value?.toMutableList()?.apply {
        findReplace(newItem) { it.isSameAs(newItem) }
      }
    }
    scrollState.update { Event(false) }
  }

  val uiState = combine(
    itemsState,
    scrollState,
    sortOrderState,
    loadingState,
    overscrollState,
  ) { s1, s2, s3, s4, s5 ->
    ProgressUiState(
      items = s1,
      scrollReset = s2,
      sortOrder = s3,
      isLoading = s4,
      isOverScrollEnabled = s5,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
    initialValue = ProgressUiState(),
  )
}
