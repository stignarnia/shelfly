package xyz.stignarnia.uiSettings.sections.backup

import com.google.common.truth.Truth.assertThat
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import xyz.stignarnia.commonTest.MainDispatcherRule
import xyz.stignarnia.dataWebdav.WebDavClient
import xyz.stignarnia.dataWebdav.WebDavCredentials
import xyz.stignarnia.repository.settings.SettingsSyncRepository
import xyz.stignarnia.repository.settings.SettingsWebDavRepository
import xyz.stignarnia.uiBackup.features.sync.SyncDevicesUseCase
import xyz.stignarnia.uiBackup.features.sync.model.SyncDeviceInfo
import xyz.stignarnia.uiModel.BackupTarget
import java.io.IOException

class SettingsBackupViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  @MockK
  lateinit var webDavRepository: SettingsWebDavRepository

  @RelaxedMockK
  lateinit var webDavClient: WebDavClient

  @MockK
  lateinit var syncRepository: SettingsSyncRepository

  @RelaxedMockK
  lateinit var syncDevicesUseCase: SyncDevicesUseCase

  private lateinit var SUT: SettingsBackupViewModel

  @Before
  fun setUp() {
    MockKAnnotations.init(this)
    clearAllMocks()

    every { webDavRepository.url } returns "https://example.com/dav/"
    every { webDavRepository.username } returns "user"
    every { webDavRepository.password } returns "pass"
    every { webDavRepository.backupTarget } returns BackupTarget.WEBDAV
    every { webDavRepository.backupRetention } returns 5
    every { webDavRepository.url = any() } returns Unit
    every { webDavRepository.username = any() } returns Unit
    every { webDavRepository.password = any() } returns Unit
    every { webDavRepository.backupTarget = any() } returns Unit
    every { webDavRepository.backupRetention = any() } returns Unit

    every { syncRepository.deviceName } returns "Pixel 8"
    every { syncRepository.deviceName = any() } returns Unit

    SUT =
      SettingsBackupViewModel(
        webDavRepository = webDavRepository,
        webDavClient = webDavClient,
        syncRepository = syncRepository,
        syncDevicesUseCase = syncDevicesUseCase,
      )
  }

  @Test
  fun `refresh should populate deviceName from sync repository`() {
    SUT.refresh()

    assertThat(SUT.uiState.value.deviceName).isEqualTo("Pixel 8")
  }

  @Test
  fun `setDeviceName should update repository and state`() {
    every { syncRepository.deviceName } returns "New Phone"

    SUT.setDeviceName("New Phone")

    verify(exactly = 1) { syncRepository.deviceName = "New Phone" }
    assertThat(SUT.uiState.value.deviceName).isEqualTo("New Phone")
  }

  @Test
  fun `loadDevices should update state with loaded devices on success`() =
    runTest {
      val expectedDevices =
        listOf(
          SyncDeviceInfo(deviceId = "dev1", deviceName = "Pixel 8", updatedAt = 1000L, isCurrentDevice = true),
          SyncDeviceInfo(deviceId = "dev2", deviceName = "Tablet", updatedAt = 900L, isCurrentDevice = false),
        )
      coEvery { syncDevicesUseCase.listDevices(any()) } returns Result.success(expectedDevices)

      SUT.loadDevices()

      val state = SUT.uiState.value.devicesLoadState
      assertThat(state).isInstanceOf(DevicesLoadState.Loaded::class.java)
      assertThat((state as DevicesLoadState.Loaded).devices).isEqualTo(expectedDevices)
    }

  @Test
  fun `loadDevices should update state with error on failure`() =
    runTest {
      coEvery { syncDevicesUseCase.listDevices(any()) } returns Result.failure(IOException("unreachable"))

      SUT.loadDevices()

      val state = SUT.uiState.value.devicesLoadState
      assertThat(state).isInstanceOf(DevicesLoadState.Error::class.java)
    }

  @Test
  fun `deleteDevice should call use case and reload devices on success`() =
    runTest {
      val credentials = WebDavCredentials("https://example.com/dav/", "user", "pass")
      coEvery { syncDevicesUseCase.deleteDevice(credentials, "dev2") } returns Result.success(Unit)
      coEvery { syncDevicesUseCase.listDevices(credentials) } returns Result.success(emptyList())

      var completed = false
      SUT.deleteDevice("dev2") { completed = it }

      assertThat(completed).isTrue()
      coVerify(exactly = 1) { syncDevicesUseCase.deleteDevice(credentials, "dev2") }
      coVerify(atLeast = 1) { syncDevicesUseCase.listDevices(credentials) }
    }
}
