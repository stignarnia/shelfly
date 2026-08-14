package xyz.stignarnia.ui_backup.features.sync

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import org.junit.Test
import xyz.stignarnia.data_webdav.WebDavClient
import xyz.stignarnia.data_webdav.WebDavCredentials
import xyz.stignarnia.data_webdav.WebDavError
import xyz.stignarnia.data_webdav.WebDavFile
import xyz.stignarnia.ui_backup.features.sync.model.SyncEntity
import xyz.stignarnia.ui_backup.features.sync.model.SyncPayload
import xyz.stignarnia.ui_backup.features.sync.model.SyncTombstoneEntry
import xyz.stignarnia.ui_backup.model.BackupLists
import xyz.stignarnia.ui_backup.model.BackupMovies
import xyz.stignarnia.ui_backup.model.BackupScheme
import xyz.stignarnia.ui_backup.model.BackupShow
import xyz.stignarnia.ui_backup.model.BackupShows

class SyncRemoteSourceTest {

  private val credentials = WebDavCredentials("https://example.com/backups/", "user", "pass")

  @Test
  fun `Should read every peer file and skip its own`() =
    runTest {
      val client = FakeWebDavClient(
        files = listOf("shelfly_sync_phone.json", "shelfly_sync_tablet.json", "shelfly_sync_laptop.json"),
        contents = mapOf(
          "shelfly_sync_tablet.json" to json(payload("tablet")),
          "shelfly_sync_laptop.json" to json(payload("laptop")),
        ),
      )

      val peers = SyncRemoteSource(client).downloadPeers(credentials, ownDeviceId = "phone")

      assertThat(peers.map { it.deviceId }).containsExactly("tablet", "laptop")
    }

  @Test
  fun `Should ignore snapshot backups sitting in the same directory`() =
    runTest {
      val client = FakeWebDavClient(
        files = listOf("shelfly_export_20260101000000.json", "shelfly_sync_tablet.json"),
        contents = mapOf("shelfly_sync_tablet.json" to json(payload("tablet"))),
      )

      val peers = SyncRemoteSource(client).downloadPeers(credentials, ownDeviceId = "phone")

      assertThat(peers.map { it.deviceId }).containsExactly("tablet")
    }

  @Test
  fun `Should skip a corrupt peer rather than failing the whole sync`() =
    runTest {
      // One device writing something malformed should cost its own changes,
      // not everyone else's. Skipping is safe because absence never deletes.
      val client = FakeWebDavClient(
        files = listOf("shelfly_sync_broken.json", "shelfly_sync_tablet.json"),
        contents = mapOf(
          "shelfly_sync_broken.json" to "{not json at all",
          "shelfly_sync_tablet.json" to json(payload("tablet")),
        ),
      )

      val peers = SyncRemoteSource(client).downloadPeers(credentials, ownDeviceId = "phone")

      assertThat(peers.map { it.deviceId }).containsExactly("tablet")
    }

  @Test
  fun `Should return no peers when the directory cannot be listed`() =
    runTest {
      val client = FakeWebDavClient(listFails = true)

      val peers = SyncRemoteSource(client).downloadPeers(credentials, ownDeviceId = "phone")

      assertThat(peers).isEmpty()
    }

  @Test
  fun `Should report never having synced when this device has no file yet`() =
    runTest {
      val client = FakeWebDavClient(files = emptyList())

      val own = SyncRemoteSource(client).downloadOwn(credentials, deviceId = "phone")

      assertThat(own).isNull()
    }

  @Test
  fun `Should round trip a payload including its tombstones`() =
    runTest {
      val client = FakeWebDavClient()
      val source = SyncRemoteSource(client)
      val original = payload(
        deviceId = "phone",
        tombstones = listOf(SyncTombstoneEntry(SyncEntity.MY_SHOW, "42", 1_700_000_000_000L)),
      )

      source.upload(credentials, original)
      val readBack = source.downloadOwn(credentials, "phone")

      assertThat(readBack?.deviceId).isEqualTo("phone")
      assertThat(readBack?.tombstones?.single()?.entity).isEqualTo(SyncEntity.MY_SHOW)
      assertThat(readBack?.tombstones?.single()?.key).isEqualTo("42")
      assertThat(
        readBack
          ?.state
          ?.shows
          ?.collectionHistory
          ?.map { it.tmdbId },
      ).containsExactly(1L)
    }

  @Test
  fun `Should write to the file named after the device`() =
    runTest {
      val client = FakeWebDavClient()

      SyncRemoteSource(client).upload(credentials, payload("tablet"))

      assertThat(client.written.keys).containsExactly("shelfly_sync_tablet.json")
    }

  private fun payload(
    deviceId: String,
    tombstones: List<SyncTombstoneEntry> = emptyList(),
  ) = SyncPayload(
    deviceId = deviceId,
    updatedAt = 1_700_000_000_000L,
    state = BackupScheme(
      version = 3,
      platform = "android",
      createdAt = "2026-01-01T00:00:00Z",
      shows = BackupShows(
        collectionHistory = listOf(
          BackupShow(1, "Show", "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z"),
        ),
      ),
      movies = BackupMovies(),
      lists = BackupLists(),
    ),
    tombstones = tombstones,
  )

  private fun json(payload: SyncPayload): String =
    Moshi
      .Builder()
      .add(KotlinJsonAdapterFactory())
      .build()
      .adapter(SyncPayload::class.java)
      .toJson(payload)
}

private class FakeWebDavClient(
  private val files: List<String> = emptyList(),
  private val contents: Map<String, String> = emptyMap(),
  private val listFails: Boolean = false,
) : WebDavClient {

  val written = mutableMapOf<String, String>()

  override suspend fun testConnection(credentials: WebDavCredentials) = Result.success(Unit)

  override suspend fun put(
    credentials: WebDavCredentials,
    fileName: String,
    content: String,
  ): Result<Unit> {
    written[fileName] = content
    return Result.success(Unit)
  }

  override suspend fun get(
    credentials: WebDavCredentials,
    fileName: String,
  ): Result<String> {
    val body = written[fileName] ?: contents[fileName]
    return body?.let { Result.success(it) } ?: Result.failure(WebDavError.NotFound)
  }

  override suspend fun list(credentials: WebDavCredentials): Result<List<WebDavFile>> {
    if (listFails) return Result.failure(WebDavError.Unreachable(java.io.IOException("offline")))
    return Result.success(files.map { WebDavFile(it, 0) })
  }

  override suspend fun delete(
    credentials: WebDavCredentials,
    fileName: String,
  ) = Result.success(Unit)
}
