package xyz.stignarnia.uiSettings.sections.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.stignarnia.dataWebdav.WebDavClient
import xyz.stignarnia.dataWebdav.WebDavCredentials
import xyz.stignarnia.repository.settings.SettingsSyncRepository
import xyz.stignarnia.repository.settings.SettingsWebDavRepository
import xyz.stignarnia.uiBackup.BackupFailure
import xyz.stignarnia.uiBackup.features.sync.SyncDevicesUseCase
import xyz.stignarnia.uiModel.BackupTarget
import javax.inject.Inject

@HiltViewModel
class SettingsBackupViewModel
  @Inject
  constructor(
    private val webDavRepository: SettingsWebDavRepository,
    private val webDavClient: WebDavClient,
    private val syncRepository: SettingsSyncRepository,
    private val syncDevicesUseCase: SyncDevicesUseCase,
  ) : ViewModel() {
    private val state = MutableStateFlow(SettingsBackupUiState())
    val uiState = state.asStateFlow()

    fun refresh() {
      state.update { current ->
        current.copy(
          webDavUrl = webDavRepository.url,
          webDavUsername = webDavRepository.username,
          hasWebDavPassword = webDavRepository.password.isNotBlank(),
          backupTarget = webDavRepository.backupTarget,
          backupRetention = webDavRepository.backupRetention,
          deviceName = syncRepository.deviceName,
        )
      }
    }

    /**
     * Updates the human-readable name identifying this device to peers.
     */
    fun setDeviceName(name: String) {
      syncRepository.deviceName = name
      refresh()
    }

    /**
     * A blank [password] means "leave the stored one alone".
     * The dialog never shows the saved password, so an empty field is the normal state when the user is only correcting the URL - treating it as a deletion would quietly break working credentials.
     */
    fun saveWebDav(
      url: String,
      username: String,
      password: String,
    ) {
      webDavRepository.url = url.trim()
      webDavRepository.username = username.trim()
      if (password.isNotBlank()) {
        webDavRepository.password = password
      }
      // Configuring a server is taken as choosing to use it, otherwise pull to sync stays inert until the user also finds the target setting.
      // Local folder can still be picked afterwards.
      if (webDavRepository.url.isNotBlank()) {
        webDavRepository.backupTarget = BackupTarget.WEBDAV
      }
      refresh()
    }

    /**
     * Stores how many backups to keep.
     * Zero means keep everything, so callers must pass a non-negative value; anything lower is refused rather than silently clamped, since deleting backups is not something to guess at.
     */
    fun setBackupRetention(count: Int) {
      if (count < SettingsWebDavRepository.RETENTION_KEEP_ALL) return
      webDavRepository.backupRetention = count
      refresh()
    }

    fun setBackupTarget(target: BackupTarget) {
      webDavRepository.backupTarget = target
      refresh()
    }

    /**
     * Tests the credentials being typed rather than the stored ones, so the user can check a change before committing to it.
     */
    fun testConnection(
      url: String,
      username: String,
      password: String,
    ) {
      if (state.value.connectionTest == ConnectionTest.Testing) return

      viewModelScope.launch {
        state.update { it.copy(connectionTest = ConnectionTest.Testing) }

        val credentials = WebDavCredentials(url.trim(), username.trim(), password)
        val result = webDavClient.testConnection(credentials)

        state.update {
          it.copy(
            connectionTest =
              result.fold(
                onSuccess = { ConnectionTest.Succeeded },
                onFailure = { error -> ConnectionTest.Failed(BackupFailure.of(error).messageRes) },
              ),
          )
        }
      }
    }

    fun clearConnectionTest() {
      state.update { it.copy(connectionTest = ConnectionTest.Idle) }
    }

    private fun webDavCredentials(): WebDavCredentials? {
      val url = webDavRepository.url.trim()
      if (url.isBlank()) return null
      return WebDavCredentials(
        url = url,
        username = webDavRepository.username.trim(),
        password = webDavRepository.password,
      )
    }

    fun loadDevices() {
      val credentials = webDavCredentials() ?: return
      viewModelScope.launch {
        state.update { it.copy(devicesLoadState = DevicesLoadState.Loading) }
        val result = syncDevicesUseCase.listDevices(credentials)
        state.update {
          it.copy(
            devicesLoadState =
              result.fold(
                onSuccess = { list -> DevicesLoadState.Loaded(list) },
                onFailure = { error -> DevicesLoadState.Error(BackupFailure.of(error).messageRes) },
              ),
          )
        }
      }
    }

    fun deleteDevice(
      deviceId: String,
      onComplete: (Boolean) -> Unit = {},
    ) {
      val credentials = webDavCredentials() ?: return
      viewModelScope.launch {
        val result = syncDevicesUseCase.deleteDevice(credentials, deviceId)
        val success = result.isSuccess
        if (success) {
          loadDevices()
        }
        onComplete(success)
      }
    }
  }
