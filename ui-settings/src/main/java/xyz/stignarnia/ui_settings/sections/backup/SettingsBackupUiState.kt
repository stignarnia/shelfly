package xyz.stignarnia.ui_settings.sections.backup

import androidx.annotation.StringRes
import xyz.stignarnia.ui_model.BackupTarget

data class SettingsBackupUiState(
  val webDavUrl: String = "",
  val webDavUsername: String = "",
  val hasWebDavPassword: Boolean = false,
  val backupTarget: BackupTarget = BackupTarget.LOCAL_FOLDER,
  val backupRetention: Int = 5,
  val connectionTest: ConnectionTest = ConnectionTest.Idle,
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
