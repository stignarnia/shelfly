package xyz.stignarnia.repository.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import xyz.stignarnia.repository.utilities.LongPreference
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * This device's identity and progress in multi-device sync.
 *
 * The id names the file this device owns on the server. Every device writes
 * only its own file, which is what removes write conflicts from the design -
 * no locking, no ETag round trips, no lost updates when two devices sync at
 * once.
 *
 * It is a random value generated on the device, used only as a filename on the
 * user's own server. It identifies a file, not a person, and is never sent
 * anywhere else.
 */
@Singleton
class SettingsSyncRepository @Inject constructor(
  @Named("syncPreferences") private var preferences: SharedPreferences,
) {

  companion object Key {
    private const val DEVICE_ID = "SYNC_DEVICE_ID"
    private const val LAST_SYNCED_AT = "SYNC_LAST_SYNCED_AT"

    /**
     * How long a deletion is remembered. A device offline for longer than this
     * may re-add items it never learned were deleted, which is the standard
     * trade against tombstones accumulating forever.
     */
    const val TOMBSTONE_RETENTION_DAYS = 90L
  }

  /** Created on first read and stable for the life of the install. */
  val deviceId: String
    get() = preferences.getString(DEVICE_ID, null) ?: UUID
      .randomUUID()
      .toString()
      .take(12)
      .also { generated -> preferences.edit { putString(DEVICE_ID, generated) } }

  var lastSyncedAt: Long by LongPreference(preferences, LAST_SYNCED_AT, 0)
}
