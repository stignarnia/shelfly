package xyz.stignarnia.uiSettings.sections.backup

import androidx.annotation.StringRes
import xyz.stignarnia.uiBackup.features.sync.model.SyncDeviceInfo
import xyz.stignarnia.uiModel.BackupTarget

data class SettingsBackupUiState(
  val webDavUrl: String = "",
  val webDavUsername: String = "",
  val hasWebDavPassword: Boolean = false,
  val backupTarget: BackupTarget = BackupTarget.LOCAL_FOLDER,
  val backupRetention: Int = 5,
  val connectionTest: ConnectionTest = ConnectionTest.Idle,
  val deviceName: String = "",
  val devicesLoadState: DevicesLoadState = DevicesLoadState.Idle,
) {
  val isWebDavConfigured: Boolean
    get() = webDavUrl.isNotBlank()
}

/**
 * The state of the "Test connection" action.
 *
 * [Failed] carries a specific reason rather than a generic error, because a wrong password, a mistyped path and a rejected certificate need entirely different fixes from the user.
 */
sealed interface ConnectionTest {
  data object Idle : ConnectionTest

  data object Testing : ConnectionTest

  data object Succeeded : ConnectionTest

  data class Failed(
    @get:StringRes val reason: Int,
  ) : ConnectionTest
}

/**
 * The state of loading synced devices from WebDAV.
 */
sealed interface DevicesLoadState {
  data object Idle : DevicesLoadState

  data object Loading : DevicesLoadState

  data class Loaded(
    val devices: List<SyncDeviceInfo>,
  ) : DevicesLoadState

  data class Error(
    @get:StringRes val messageRes: Int,
  ) : DevicesLoadState
}
