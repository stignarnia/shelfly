package xyz.stignarnia.ui_settings.sections.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.stignarnia.data_webdav.WebDavClient
import xyz.stignarnia.data_webdav.WebDavCredentials
import xyz.stignarnia.data_webdav.WebDavError
import xyz.stignarnia.repository.settings.SettingsWebDavRepository
import xyz.stignarnia.ui_model.BackupTarget
import xyz.stignarnia.ui_settings.R
import javax.inject.Inject

@HiltViewModel
class SettingsBackupViewModel @Inject constructor(
  private val webDavRepository: SettingsWebDavRepository,
  private val webDavClient: WebDavClient,
) : ViewModel() {

  private val state = MutableStateFlow(SettingsBackupUiState())
  val uiState = state.asStateFlow()

  fun refresh() {
    state.value = SettingsBackupUiState(
      webDavUrl = webDavRepository.url,
      webDavUsername = webDavRepository.username,
      hasWebDavPassword = webDavRepository.password.isNotBlank(),
      backupTarget = webDavRepository.backupTarget,
      backupRetention = webDavRepository.backupRetention,
    )
  }

  /**
   * A blank [password] means "leave the stored one alone". The dialog never
   * shows the saved password, so an empty field is the normal state when the
   * user is only correcting the URL - treating it as a deletion would quietly
   * break working credentials.
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
    // Selecting a server is not the same as choosing to use it; the target is
    // switched deliberately, so a saved server does not silently redirect
    // backups away from the folder the user already picked.
    refresh()
  }

  /**
   * Stores how many backups to keep. Zero means keep everything, so callers
   * must pass a non-negative value; anything lower is refused rather than
   * silently clamped, since deleting backups is not something to guess at.
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
   * Tests the credentials being typed rather than the stored ones, so the user
   * can check a change before committing to it.
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
          connectionTest = result.fold(
            onSuccess = { ConnectionTest.Succeeded },
            onFailure = { error -> ConnectionTest.Failed(error.toReasonRes()) },
          ),
        )
      }
    }
  }

  fun clearConnectionTest() {
    state.update { it.copy(connectionTest = ConnectionTest.Idle) }
  }

  private fun Throwable.toReasonRes(): Int =
    when (this) {
      is WebDavError.Unauthorized -> R.string.textSettingsWebDavErrorAuth
      is WebDavError.NotFound -> R.string.textSettingsWebDavErrorNotFound
      is WebDavError.TlsFailure -> R.string.textSettingsWebDavErrorTls
      is WebDavError.Unreachable -> R.string.textSettingsWebDavErrorUnreachable
      else -> R.string.textSettingsWebDavErrorUnexpected
    }
}
