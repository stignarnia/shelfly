package xyz.stignarnia.repository.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import xyz.stignarnia.common.security.SecretCipher
import xyz.stignarnia.repository.utilities.EnumPreference
import xyz.stignarnia.repository.utilities.StringPreference
import xyz.stignarnia.ui_model.BackupTarget
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Stores the WebDAV backup destination and which target scheduled backups use.
 *
 * The password goes through [SecretCipher], so it is held as Keystore-encrypted
 * ciphertext rather than plain text in SharedPreferences. The URL and username
 * are not secret and are stored as-is, which keeps them readable when
 * diagnosing a failing backup.
 */
@Singleton
class SettingsWebDavRepository @Inject constructor(
  @Named("webdavPreferences") private val preferences: SharedPreferences,
  private val secretCipher: SecretCipher,
) {

  companion object Key {
    private const val URL = "WEBDAV_URL"
    private const val USERNAME = "WEBDAV_USERNAME"
    private const val PASSWORD = "WEBDAV_PASSWORD"
    private const val TARGET = "BACKUP_TARGET"
  }

  var url: String by StringPreference(preferences, URL, "")
  var username: String by StringPreference(preferences, USERNAME, "")

  var backupTarget: BackupTarget by EnumPreference(
    preferences,
    TARGET,
    BackupTarget.LOCAL_FOLDER,
    BackupTarget::class.java,
  )

  /**
   * Returns an empty string when nothing is stored, and also when the stored
   * value cannot be decrypted - which happens if the Keystore key was lost to a
   * device restore. The user is asked for the password again in that case,
   * rather than the backup silently failing to authenticate.
   */
  var password: String
    get() {
      val stored = preferences.getString(PASSWORD, null) ?: return ""
      return secretCipher.decrypt(stored) ?: ""
    }
    set(value) {
      preferences.edit {
        if (value.isBlank()) {
          remove(PASSWORD)
        } else {
          putString(PASSWORD, secretCipher.encrypt(value))
        }
      }
    }

  fun clear() {
    preferences.edit { clear() }
  }
}
