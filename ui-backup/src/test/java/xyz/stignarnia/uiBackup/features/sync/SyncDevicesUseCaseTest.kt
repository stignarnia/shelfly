package xyz.stignarnia.uiBackup.features.sync

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import xyz.stignarnia.dataWebdav.WebDavClient
import xyz.stignarnia.dataWebdav.WebDavCredentials
import xyz.stignarnia.dataWebdav.WebDavFile
import xyz.stignarnia.repository.settings.SettingsSyncRepository
import xyz.stignarnia.uiBackup.features.sync.model.SyncDeviceHeader

class SyncDevicesUseCaseTest {
  @RelaxedMockK
  lateinit var webDavClient: WebDavClient

  @MockK
  lateinit var settingsSyncRepository: SettingsSyncRepository

  private lateinit var SUT: SyncDevicesUseCase

  private val headerAdapter = Moshi.Builder().build().adapter(SyncDeviceHeader::class.java)

  @Before
  fun setUp() {
    MockKAnnotations.init(this)
    clearAllMocks()

    every { settingsSyncRepository.deviceId } returns OWN_DEVICE_ID
    every { settingsSyncRepository.deviceName } returns "My Pixel"
    every { settingsSyncRepository.lastSyncedAt } returns 1000L

    SUT = SyncDevicesUseCase(webDavClient, settingsSyncRepository)
  }

  @Test
  fun `listDevices should return empty list when no sync files exist`() =
    runTest {
      val files =
        listOf(
          WebDavFile("shelfly_export_2026-01-01.json", lastModifiedMillis = 500L),
          WebDavFile("other_file.txt", lastModifiedMillis = 200L),
        )
      coEvery { webDavClient.list(CREDENTIALS) } returns Result.success(files)

      val result = SUT.listDevices(CREDENTIALS)

      assertThat(result.isSuccess).isTrue()
      assertThat(result.getOrThrow()).isEmpty()
    }

  @Test
  fun `listDevices should list and sort devices with current device first and peers by updatedAt descending`() =
    runTest {
      val files =
        listOf(
          WebDavFile("shelfly_sync_peer1.json", lastModifiedMillis = 200L),
          WebDavFile("shelfly_sync_$OWN_DEVICE_ID.json", lastModifiedMillis = 500L),
          WebDavFile("shelfly_sync_peer2.json", lastModifiedMillis = 300L),
        )
      coEvery { webDavClient.list(CREDENTIALS) } returns Result.success(files)

      val peer1Header =
        headerAdapter.toJson(SyncDeviceHeader(deviceId = "peer1", deviceName = "Tablet", updatedAt = 2000L))
      val ownHeader =
        headerAdapter.toJson(SyncDeviceHeader(deviceId = OWN_DEVICE_ID, deviceName = "My Pixel", updatedAt = 5000L))
      val peer2Header =
        headerAdapter.toJson(SyncDeviceHeader(deviceId = "peer2", deviceName = "Laptop", updatedAt = 8000L))

      coEvery { webDavClient.get(CREDENTIALS, "shelfly_sync_peer1.json") } returns Result.success(peer1Header)
      coEvery { webDavClient.get(CREDENTIALS, "shelfly_sync_$OWN_DEVICE_ID.json") } returns Result.success(ownHeader)
      coEvery { webDavClient.get(CREDENTIALS, "shelfly_sync_peer2.json") } returns Result.success(peer2Header)

      val result = SUT.listDevices(CREDENTIALS)

      assertThat(result.isSuccess).isTrue()
      val list = result.getOrThrow()
      assertThat(list).hasSize(3)

      // Current device must be first
      assertThat(list[0].deviceId).isEqualTo(OWN_DEVICE_ID)
      assertThat(list[0].deviceName).isEqualTo("My Pixel")
      assertThat(list[0].isCurrentDevice).isTrue()

      // Peers sorted by updatedAt descending: peer2 (8000L), then peer1 (2000L)
      assertThat(list[1].deviceId).isEqualTo("peer2")
      assertThat(list[1].deviceName).isEqualTo("Laptop")
      assertThat(list[1].isCurrentDevice).isFalse()

      assertThat(list[2].deviceId).isEqualTo("peer1")
      assertThat(list[2].deviceName).isEqualTo("Tablet")
      assertThat(list[2].isCurrentDevice).isFalse()
    }

  @Test
  fun `listDevices should fallback gracefully when file header is unparseable`() =
    runTest {
      val files = listOf(WebDavFile("shelfly_sync_unknown123.json", lastModifiedMillis = 400L))
      coEvery { webDavClient.list(CREDENTIALS) } returns Result.success(files)
      coEvery { webDavClient.get(CREDENTIALS, "shelfly_sync_unknown123.json") } returns Result.success("invalid json")

      val result = SUT.listDevices(CREDENTIALS)

      assertThat(result.isSuccess).isTrue()
      val list = result.getOrThrow()
      assertThat(list).hasSize(1)
      assertThat(list[0].deviceId).isEqualTo("unknown123")
      assertThat(list[0].deviceName).isEqualTo("Device (unknow)")
      assertThat(list[0].updatedAt).isEqualTo(400L)
      assertThat(list[0].isCurrentDevice).isFalse()
    }

  @Test
  fun `deleteDevice should call webDavClient delete with sync file name`() =
    runTest {
      coEvery { webDavClient.delete(CREDENTIALS, "shelfly_sync_peer1.json") } returns Result.success(Unit)

      val result = SUT.deleteDevice(CREDENTIALS, "peer1")

      assertThat(result.isSuccess).isTrue()
      coVerify(exactly = 1) { webDavClient.delete(CREDENTIALS, "shelfly_sync_peer1.json") }
    }

  private companion object {
    const val OWN_DEVICE_ID = "phone123"
    val CREDENTIALS = WebDavCredentials("https://example.com/backups/", "user", "pass")
  }
}
