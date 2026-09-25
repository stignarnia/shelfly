package xyz.stignarnia.uiBackup.features.sync.model

/**
 * Information describing a device whose sync file is present on the WebDAV server.
 */
data class SyncDeviceInfo(
  val deviceId: String,
  val deviceName: String,
  val updatedAt: Long,
  val isCurrentDevice: Boolean,
)
