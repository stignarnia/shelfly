package xyz.stignarnia.shelfly.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.stignarnia.common.Mode
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.shelfly.ui.main.cases.MainAnnouncementsCase
import xyz.stignarnia.shelfly.ui.main.cases.MainBackupCase
import xyz.stignarnia.shelfly.ui.main.cases.MainClearingCase
import xyz.stignarnia.shelfly.ui.main.cases.MainInitialsCase
import xyz.stignarnia.shelfly.ui.main.cases.MainModesCase
import xyz.stignarnia.shelfly.ui.main.cases.MainTipsCase
import xyz.stignarnia.shelfly.ui.main.cases.MainWelcomeCase
import xyz.stignarnia.shelfly.ui.main.cases.deeplink.MainDeepLinksCase
import xyz.stignarnia.shelfly.ui.main.welcome.WelcomeState
import xyz.stignarnia.shelfly.ui.main.welcome.WelcomeStep
import xyz.stignarnia.shelfly.utilities.deeplink.DeepLinkBundle
import xyz.stignarnia.shelfly.utilities.deeplink.DeepLinkSource
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.uiBase.utilities.extensions.combine
import xyz.stignarnia.uiBase.utilities.extensions.launchDelayed
import xyz.stignarnia.uiBase.utilities.extensions.rethrowCancellation
import xyz.stignarnia.uiModel.Tip
import javax.inject.Inject

@HiltViewModel
class MainViewModel
  @Inject
  constructor(
    private val initCase: MainInitialsCase,
    private val welcomeCase: MainWelcomeCase,
    private val tipsCase: MainTipsCase,
    private val backupCase: MainBackupCase,
    private val clearingCase: MainClearingCase,
    private val announcementsCase: MainAnnouncementsCase,
    private val modesCase: MainModesCase,
    private val linksCase: MainDeepLinksCase,
    private val settingsRepository: SettingsRepository,
  ) : ViewModel() {
    private val loadingState = MutableStateFlow(false)
    private val maskState = MutableStateFlow(false)
    private val welcomeState = MutableStateFlow<WelcomeState?>(null)
    private val welcomeResolvedState = MutableStateFlow(false)
    private val showDiscoverEvent = MutableStateFlow<Event<Boolean>?>(null)
    private val requestNotificationsEvent = MutableStateFlow<Event<Boolean>?>(null)
    private val openSettingsEvent = MutableStateFlow<Event<Boolean>?>(null)
    private val openLinkEvent = MutableStateFlow<Event<DeepLinkBundle>?>(null)

    private var welcomeQueue: List<WelcomeStep> = emptyList()
    private var isFirstRunFlow = false

    /** Kept per step, so the TMDB key does not turn up prefilled on the OMDb step. */
    private val apiKeyDrafts = mutableMapOf<String, String>()

    /**
     * The activity calls [initialize] from every onCreate, but the queue must only be built once per view model: applying a language restarts the activity, and rebuilding would drop the user back to the first step.
     */
    private var isInitialized = false

    fun initialize() {
      if (isInitialized) return
      isInitialized = true
      viewModelScope.launch {
        try {
          val isInitialRun = checkInitialRun()
          with(initCase) {
            saveInstallTimestamp()
          }
          startWelcomeFlow(isInitialRun)
        } finally {
          // The first frame is held until this flips, so a failure above has to release it too rather than leave the app on the launch window.
          welcomeResolvedState.value = true
        }
      }
    }

    private suspend fun checkInitialRun(): Boolean {
      val isInitialRun = initCase.isInitialRun()
      if (isInitialRun) {
        initCase.setInitialRun(false)
        initCase.setInitialNotifications()
        initCase.setInitialCountry()
      }
      return isInitialRun
    }

    private fun startWelcomeFlow(isInitialRun: Boolean) {
      welcomeQueue = welcomeCase.buildQueue(isInitialRun)
      isFirstRunFlow = isInitialRun
      if (welcomeQueue.isEmpty()) {
        finishWelcomeFlow()
        return
      }
      showWelcomeStep(0)
    }

    private fun showWelcomeStep(index: Int) {
      val step = welcomeQueue.getOrNull(index)
      welcomeState.value =
        step?.let {
          WelcomeState(
            step = it,
            index = index,
            total = welcomeQueue.size,
            displayLanguage = welcomeCase.currentLanguage(),
            apiKeyDraft = apiKeyDrafts[it.id].orEmpty(),
          )
        }
      if (step == null) {
        finishWelcomeFlow()
      }
    }

    /**
     * Discover is only opened once the flow is done, never while it runs: it fetches from TMDB, and before the key step there is no key to fetch with - which used to leave a load failure sitting behind the welcome screens.
     */
    private fun finishWelcomeFlow() {
      if (!isFirstRunFlow) return
      isFirstRunFlow = false
      // Discover is only where a first run lands by default.
      // A user who asked for the sync setup on the way out has already said where they want to be, and this would otherwise be navigated straight over the top of it.
      if (openSettingsEvent.value != null) return
      showDiscoverEvent.value = Event(true)
    }

    fun onWelcomePrimary() {
      val state = welcomeState.value ?: return
      when (val step = state.step) {
        is WelcomeStep.Language -> {
          welcomeCase.setLanguage(step.suggested)
        }

        is WelcomeStep.ApiKey -> {
          val key = apiKeyDrafts[step.id].orEmpty().trim()
          if (key.isBlank()) return
          when (step) {
            is WelcomeStep.ApiKey.Tmdb -> welcomeCase.setTmdbApiKey(key)
            is WelcomeStep.ApiKey.Omdb -> welcomeCase.setOmdbApiKey(key)
          }
        }

        is WelcomeStep.Notifications -> {
          // The grant is the activity's to ask for; the step advances only once the answer is back, in onNotificationsPermissionResult.
          requestNotificationsEvent.value = Event(true)
          return
        }

        is WelcomeStep.WebDavSync -> {
          openSettingsEvent.value = Event(true)
        }

        else -> { }
      }
      completeWelcomeStep(state)
    }

    fun onWelcomeSecondary() {
      val state = welcomeState.value ?: return
      when (val step = state.step) {
        // Declining still has to pin the current language explicitly: with no app locale applied, the device language would win at resource lookup.
        is WelcomeStep.Language -> {
          welcomeCase.setLanguage(step.current)
        }

        // Declining is itself the answer; the step just needs to be recorded.
        is WelcomeStep.ApiKey.Omdb, is WelcomeStep.Notifications, is WelcomeStep.WebDavSync -> { }

        else -> {
          return
        }
      }
      completeWelcomeStep(state)
    }

    fun onNotificationsPermissionResult(isGranted: Boolean) {
      val state = welcomeState.value ?: return
      if (state.step !is WelcomeStep.Notifications) return
      viewModelScope.launch {
        if (isGranted) {
          welcomeCase.setNotificationsEnabled(true)
          announcementsCase.refreshAnnouncements()
        }
        completeWelcomeStep(state)
      }
    }

    /**
     * Returns whether the welcome flow handled the gesture.
     * The flow itself cannot be dismissed, so the first step swallows back rather than letting it through.
     */
    fun onWelcomeBack(): Boolean {
      val state = welcomeState.value ?: return false
      if (state.isBackEnabled) {
        showWelcomeStep(state.index - 1)
      }
      return true
    }

    fun onApiKeyDraftChanged(value: String) {
      val state = welcomeState.value ?: return
      if (state.step !is WelcomeStep.ApiKey) return
      apiKeyDrafts[state.step.id] = value
      welcomeState.value = state.copy(apiKeyDraft = value)
    }

    private fun completeWelcomeStep(state: WelcomeState) {
      welcomeCase.setStepCompleted(state.step)
      showWelcomeStep(state.index + 1)
    }

    fun refreshAnnouncements() {
      viewModelScope.launch {
        announcementsCase.refreshAnnouncements()
      }
    }

    fun refreshBackupExportSchedule() {
      backupCase.run {
        refreshBackupExportSchedule()
      }
    }

    fun setMode(mode: Mode) = modesCase.setMode(mode)

    fun getMode(): Mode = modesCase.getMode()

    fun isTipShown(tip: Tip) = tipsCase.isTipShown(tip)

    fun setTipShown(tip: Tip) = tipsCase.setTipShown(tip)

    fun hasMoviesEnabled(): Boolean = settingsRepository.isMoviesEnabled

    fun openDeepLink(source: DeepLinkSource) {
      viewModelScope.launch {
        val progressJob =
          launchDelayed(750) {
            loadingState.value = true
            maskState.value = true
          }
        try {
          val result =
            when (source) {
              is DeepLinkSource.ImdbSource -> linksCase.findById(source.id)
              is DeepLinkSource.TmdbSource -> linksCase.findById(source.id, source.type)
            }
          loadingState.value = false
          maskState.value = false
          openLinkEvent.value = Event(result)
        } catch (error: Throwable) {
          rethrowCancellation(error)
        } finally {
          progressJob.cancelAndJoin()
        }
      }
    }

    override fun onCleared() {
      clearingCase.clear()
    }

    val uiState =
      combine(
        welcomeResolvedState,
        welcomeState,
        showDiscoverEvent,
        requestNotificationsEvent,
        openSettingsEvent,
        openLinkEvent,
        loadingState,
        maskState,
      ) { welcomeResolved, welcome, discover, notifications, settings, link, loading, mask ->
        MainUiState(
          isWelcomeResolved = welcomeResolved,
          welcome = welcome,
          showDiscover = discover,
          requestNotifications = notifications,
          openSettings = settings,
          openLink = link,
          isLoading = loading,
          showMask = mask,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
        initialValue = MainUiState(),
      )
  }
