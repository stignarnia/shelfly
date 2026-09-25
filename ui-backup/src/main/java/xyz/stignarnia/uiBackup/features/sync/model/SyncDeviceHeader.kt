package xyz.stignarnia.uiBackup.features.sync.model

import com.squareup.moshi.JsonClass

/**
 * Lightweight header extracted from a remote sync payload.
 *
 * Moshi skips the full collection/movie/show state, allowing rapid directory inspection
 * without allocating large objects in memory.
 */
@JsonClass(generateAdapter = true)
data class SyncDeviceHeader(
  val deviceId: String = "",
  val deviceName: String? = null,
  val updatedAt: Long = 0L,
)
