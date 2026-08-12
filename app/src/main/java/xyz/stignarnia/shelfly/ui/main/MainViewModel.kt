package xyz.stignarnia.shelfly.ui.main

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import xyz.stignarnia.common.Mode
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.shelfly.ui.main.cases.MainAnnouncementsCase
import xyz.stignarnia.shelfly.ui.main.cases.MainBackupCase
import xyz.stignarnia.shelfly.ui.main.cases.MainClearingCase
import xyz.stignarnia.shelfly.ui.main.cases.MainInitialsCase
import xyz.stignarnia.shelfly.ui.main.cases.MainModesCase
import xyz.stignarnia.shelfly.ui.main.cases.MainTipsCase
import xyz.stignarnia.shelfly.ui.main.cases.deeplink.MainDeepLinksCase
import xyz.stignarnia.shelfly.utilities.deeplink.DeepLinkBundle
import xyz.stignarnia.shelfly.utilities.deeplink.DeepLinkSource
import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.ui_base.utilities.extensions.combine
import xyz.stignarnia.ui_base.utilities.extensions.launchDelayed
import xyz.stignarnia.ui_base.utilities.extensions.rethrowCancellation
import xyz.stignarnia.ui_model.Tip
import xyz.stignarnia.ui_settings.helpers.AppLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class MainViewModel @Inject constructor(
  private val initCase: MainInitialsCase,
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
  private val initialRunEvent = MutableStateFlow<Event<Boolean>?>(null)
  private val initialLanguageEvent = MutableStateFlow<Event<AppLanguage>?>(null)
  private val whatsNewEvent = MutableStateFlow<Event<Boolean>?>(null)
  private val openLinkEvent = MutableStateFlow<Event<DeepLinkBundle>?>(null)

  fun initialize() {
    viewModelScope.launch {
      val isInitialRun = checkInitialRun()
      with(initCase) {
        saveInstallTimestamp()
      }
      checkApi13Locale(isInitialRun)
    }
  }

  private suspend fun checkInitialRun(): Boolean {
    val isInitialRun = initCase.isInitialRun()
    if (isInitialRun) {
      initCase.setInitialRun(false)
      initCase.setInitialNotifications()
      initCase.setInitialCountry()
    }

    val showWhatsNew = initCase.showWhatsNew(isInitialRun)

    initialRunEvent.value = Event(isInitialRun)
    whatsNewEvent.value = Event(showWhatsNew)

    return isInitialRun
  }

  fun setLanguage(appLanguage: AppLanguage) = initCase.setLanguage(appLanguage)

  fun checkInitialLanguage() {
    viewModelScope.launch {
      val initialLanguage = initCase.checkInitialLanguage()
      initialLanguageEvent.value = Event(initialLanguage)
      maskState.value = true
    }
  }

  private fun checkApi13Locale(isInitialRun: Boolean) {
    if (!isInitialRun && !settingsRepository.isLocaleInitialised) {
      settingsRepository.isLocaleInitialised = true
      val locale = LocaleListCompat.forLanguageTags(settingsRepository.language)
      AppCompatDelegate.setApplicationLocales(locale)
    }
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

  fun clearMask() {
    maskState.value = false
  }

  fun openDeepLink(source: DeepLinkSource) {
    viewModelScope.launch {
      val progressJob = launchDelayed(750) {
        loadingState.value = true
        maskState.value = true
      }
      try {
        val result = when (source) {
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
    super.onCleared()
  }

  val uiState = combine(
    initialRunEvent,
    initialLanguageEvent,
    whatsNewEvent,
    openLinkEvent,
    loadingState,
    maskState,
  ) { s1, s2, s3, s4, s5, s6 ->
    MainUiState(
      isInitialRun = s1,
      initialLanguage = s2,
      showWhatsNew = s3,
      openLink = s4,
      isLoading = s5,
      showMask = s6,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
    initialValue = MainUiState(),
  )
}
