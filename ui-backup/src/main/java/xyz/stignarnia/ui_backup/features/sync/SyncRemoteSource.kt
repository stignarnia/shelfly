package xyz.stignarnia.ui_backup.features.sync

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import timber.log.Timber
import xyz.stignarnia.data_webdav.WebDavClient
import xyz.stignarnia.data_webdav.WebDavCredentials
import xyz.stignarnia.ui_backup.features.sync.model.SyncPayload
import javax.inject.Inject

/**
 * Reads and writes sync payloads on the user's WebDAV server.
 *
 * Each device writes only the file named after it, so two devices syncing at
 * the same moment never touch the same object and there is nothing to lock.
 */
internal class SyncRemoteSource @Inject constructor(
  private val webDavClient: WebDavClient,
) {

  private val adapter by lazy {
    Moshi
      .Builder()
      .add(KotlinJsonAdapterFactory())
      .build()
      .adapter(SyncPayload::class.java)
  }

  /**
   * Every peer's payload, excluding this device's own.
   *
   * A file that cannot be read or parsed is skipped rather than failing the
   * sync: one device writing something malformed should cost its own changes,
   * not everyone else's. Skipping is safe because absence never deletes.
   */
  suspend fun downloadPeers(
    credentials: WebDavCredentials,
    ownDeviceId: String,
  ): List<SyncPayload> {
    val files = webDavClient
      .list(credentials)
      .getOrElse { error ->
        Timber.w(error, "Could not list sync files")
        return emptyList()
      }

    return files
      .filter { SyncFileName.isSyncFile(it.name) }
      .filter { SyncFileName.deviceIdOf(it.name) != ownDeviceId }
      .mapNotNull { file -> read(credentials, file.name) }
  }

  /** This device's last published payload, or null if it has never synced. */
  suspend fun downloadOwn(
    credentials: WebDavCredentials,
    deviceId: String,
  ): SyncPayload? = read(credentials, SyncFileName.forDevice(deviceId))

  suspend fun upload(
    credentials: WebDavCredentials,
    payload: SyncPayload,
  ): Result<Unit> =
    webDavClient.put(
      credentials = credentials,
      fileName = SyncFileName.forDevice(payload.deviceId),
      content = adapter.toJson(payload),
    )

  private suspend fun read(
    credentials: WebDavCredentials,
    fileName: String,
  ): SyncPayload? {
    val json = webDavClient
      .get(credentials, fileName)
      .getOrElse { error ->
        Timber.w("Could not read $fileName: ${error.message}")
        return null
      }
    return try {
      adapter.fromJson(json)
    } catch (error: Exception) {
      Timber.w(error, "Could not parse $fileName")
      null
    }
  }
}
