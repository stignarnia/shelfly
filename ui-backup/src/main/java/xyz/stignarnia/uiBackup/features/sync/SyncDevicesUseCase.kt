package xyz.stignarnia.uiBackup.features.sync

import com.squareup.moshi.Moshi
import timber.log.Timber
import xyz.stignarnia.dataWebdav.WebDavClient
import xyz.stignarnia.dataWebdav.WebDavCredentials
import xyz.stignarnia.repository.settings.SettingsSyncRepository
import xyz.stignarnia.uiBackup.features.sync.model.SyncDeviceHeader
import xyz.stignarnia.uiBackup.features.sync.model.SyncDeviceInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncDevicesUseCase
  @Inject
  constructor(
    private val webDavClient: WebDavClient,
    private val settingsSyncRepository: SettingsSyncRepository,
  ) {
    private val headerAdapter by lazy {
      Moshi
        .Builder()
        .build()
        .adapter(SyncDeviceHeader::class.java)
    }

    /**
     * Lists all devices that currently have a sync payload on the WebDAV server.
     *
     * Returns devices sorted with the current device first (if present), followed by
     * devices ordered by most recent sync timestamp descending.
     */
    suspend fun listDevices(credentials: WebDavCredentials): Result<List<SyncDeviceInfo>> =
      runCatching {
        val ownId = settingsSyncRepository.deviceId
        val files = webDavClient.list(credentials).getOrThrow()
        val syncFiles = files.filter { SyncFileName.isSyncFile(it.name) }

        val devices =
          syncFiles.map { file ->
            val deviceId = SyncFileName.deviceIdOf(file.name)
            val isCurrent = deviceId == ownId
            val header = readHeader(credentials, file.name)
            val headerName = header?.deviceName?.trim()

            val displayName =
              when {
                !headerName.isNullOrBlank() -> headerName
                isCurrent -> settingsSyncRepository.deviceName
                else -> "Device (${deviceId.take(6)})"
              }

            val timestamp =
              when {
                header != null && header.updatedAt > 0L -> header.updatedAt
                file.lastModifiedMillis > 0L -> file.lastModifiedMillis
                isCurrent -> settingsSyncRepository.lastSyncedAt
                else -> 0L
              }

            SyncDeviceInfo(
              deviceId = deviceId,
              deviceName = displayName,
              updatedAt = timestamp,
              isCurrentDevice = isCurrent,
            )
          }

        devices.sortedWith(
          compareByDescending<SyncDeviceInfo> { it.isCurrentDevice }
            .thenByDescending { it.updatedAt },
        )
      }

    /**
     * Deletes the sync payload file for [deviceId] from WebDAV.
     */
    suspend fun deleteDevice(
      credentials: WebDavCredentials,
      deviceId: String,
    ): Result<Unit> = webDavClient.delete(credentials, SyncFileName.forDevice(deviceId))

    private suspend fun readHeader(
      credentials: WebDavCredentials,
      fileName: String,
    ): SyncDeviceHeader? {
      val json = webDavClient.get(credentials, fileName).getOrElse { return null }
      return try {
        headerAdapter.fromJson(json)
      } catch (e: Exception) {
        Timber.w(e, "Could not parse sync header for $fileName")
        null
      }
    }
  }
