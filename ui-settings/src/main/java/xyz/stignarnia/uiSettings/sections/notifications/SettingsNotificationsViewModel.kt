package xyz.stignarnia.uiSettings.sections.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.uiBase.viewmodel.ChannelsDelegate
import xyz.stignarnia.uiBase.viewmodel.DefaultChannelsDelegate
import xyz.stignarnia.uiModel.NotificationDelay
import xyz.stignarnia.uiModel.Settings
import xyz.stignarnia.uiSettings.sections.notifications.SettingsNotificationsUiEvent.NotificationsBlocked
import xyz.stignarnia.uiSettings.sections.notifications.cases.SettingsNotificationsMainCase
import javax.inject.Inject

@HiltViewModel
class SettingsNotificationsViewModel
  @Inject
  constructor(
    private val mainCase: SettingsNotificationsMainCase,
  ) : ViewModel(),
    ChannelsDelegate by DefaultChannelsDelegate() {
    private val settingsState = MutableStateFlow<Settings?>(null)
    private val loadingState = MutableStateFlow(false)

    fun loadSettings(context: Context) {
      viewModelScope.launch {
        areNotificationsAllowed(context)
        refreshSettings()
      }
    }

    fun enableNotifications(
      enable: Boolean,
      context: Context,
    ) {
      viewModelScope.launch {
        if (enable && !areNotificationsAllowed(context)) {
          eventChannel.send(NotificationsBlocked)
          return@launch
        }
        mainCase.enableNotifications(enable)
        refreshSettings()
      }
    }

    fun setWhenToNotify(delay: NotificationDelay) {
      viewModelScope.launch {
        mainCase.setWhenToNotify(delay)
        refreshSettings()
      }
    }

    private suspend fun refreshSettings() {
      settingsState.value = mainCase.getSettings()
    }

    /**
     * Whether the system currently lets this app post notifications.
     *
     * areNotificationsEnabled covers every version and both reasons it can be false - a permission never granted on API 33 and up, or the user switching notifications off in system settings - so the app never claims they are on while the system has them off.
     * When it says no, the stored setting is brought back in line rather than left showing a promise the system will not keep.
     */
    private suspend fun areNotificationsAllowed(context: Context): Boolean {
      val areNotificationsEnabled =
        NotificationManagerCompat
          .from(context.applicationContext)
          .areNotificationsEnabled()

      if (!areNotificationsEnabled) {
        mainCase.enableNotifications(false)
      }

      return areNotificationsEnabled
    }

    val uiState =
      combine(
        settingsState,
        loadingState,
      ) { s1, _ ->
        SettingsNotificationsUiState(
          settings = s1,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
        initialValue = SettingsNotificationsUiState(),
      )
  }
