package xyz.stignarnia.ui_backup.features.sync

import com.google.common.truth.Truth.assertThat
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import xyz.stignarnia.data_webdav.WebDavCredentials
import xyz.stignarnia.repository.settings.SettingsSyncRepository
import xyz.stignarnia.ui_backup.features.export.workers.BackupExportWorker
import xyz.stignarnia.ui_backup.features.import_.workers.BackupImportWorker
import xyz.stignarnia.ui_backup.features.sync.model.SyncEntity
import xyz.stignarnia.ui_backup.features.sync.model.SyncPayload
import xyz.stignarnia.ui_backup.features.sync.model.SyncTombstoneEntry
import xyz.stignarnia.ui_backup.model.BackupScheme
import xyz.stignarnia.ui_backup.model.BackupShow
import xyz.stignarnia.ui_backup.model.BackupShows

class SyncEngineTest {

  @RelaxedMockK lateinit var exportWorker: BackupExportWorker
  @RelaxedMockK lateinit var importWorker: BackupImportWorker
  @RelaxedMockK internal lateinit var applier: SyncStateApplier
  @RelaxedMockK internal lateinit var remoteSource: SyncRemoteSource
  @RelaxedMockK internal lateinit var tombstoneStore: SyncTombstoneStore
  @MockK lateinit var settingsSyncRepository: SettingsSyncRepository

  private lateinit var SUT: SyncEngine

  @Before
  fun setUp() {
    MockKAnnotations.init(this)
    clearAllMocks()

    every { settingsSyncRepository.deviceId } returns DEVICE
    every { settingsSyncRepository.lastSyncedAt } returns PREVIOUS_SYNC
    every { settingsSyncRepository.lastSyncedAt = any() } returns Unit
    every { settingsSyncRepository.lastAttemptAt = any() } returns Unit
    every { settingsSyncRepository.lastError = any() } returns Unit
    every { settingsSyncRepository.lastPeers = any() } returns Unit

    coEvery { exportWorker.run() } returns scheme(show(1))
    coEvery { remoteSource.downloadOwn(any(), any()) } returns null
    coEvery { remoteSource.downloadPeers(any(), any()) } returns emptyList()
    coEvery { remoteSource.upload(any(), any()) } returns Result.success(Unit)
    coEvery { tombstoneStore.refresh(any(), any(), any(), any()) } returns emptyList()

    SUT = SyncEngine(exportWorker, importWorker, applier, remoteSource, tombstoneStore, settingsSyncRepository)
  }

  @Test
  fun `Should remove before importing so the importer cannot re-add what the merge dropped`() =
    runTest {
      SUT.sync(CREDENTIALS)

      coVerifyOrder {
        applier.apply(any(), any())
        importWorker.run(any())
      }
    }

  @Test
  fun `Should date new tombstones at the previous sync rather than now`() =
    runTest {
      SUT.sync(CREDENTIALS)

      coVerify(exactly = 1) {
        tombstoneStore.refresh(any(), any(), deletedAt = PREVIOUS_SYNC, now = any())
      }
    }

  @Test
  fun `Should diff against what this device last published`() =
    runTest {
      val published = scheme(show(1), show(2))
      coEvery { remoteSource.downloadOwn(CREDENTIALS, DEVICE) } returns payload(published)

      SUT.sync(CREDENTIALS)

      coVerify(exactly = 1) {
        tombstoneStore.refresh(published = published, local = any(), deletedAt = any(), now = any())
      }
    }

  @Test
  fun `Should publish the state that actually landed, not the state it aimed for`() =
    runTest {
      // The import can fall short - a show whose details will not fetch is skipped - and this file is the next cycle's baseline.
      // Publishing the intent would make the next diff read those gaps as deletions.
      val before = scheme(show(1))
      val after = scheme(show(1), show(2))
      coEvery { exportWorker.run() } returnsMany listOf(before, after)

      SUT.sync(CREDENTIALS)

      val uploaded = slot<SyncPayload>()
      coVerify(exactly = 1) { remoteSource.upload(any(), capture(uploaded)) }
      assertThat(uploaded.captured.state).isEqualTo(after)
      coVerify(exactly = 2) { exportWorker.run() }
    }

  @Test
  fun `Should re-publish tombstones learned from peers`() =
    runTest {
      val peerTombstone = SyncTombstoneEntry(SyncEntity.MY_SHOW, "9", deletedAt = 500)
      coEvery { remoteSource.downloadPeers(any(), any()) } returns listOf(
        SyncPayload(deviceId = "tablet", updatedAt = 1, state = scheme(), tombstones = listOf(peerTombstone)),
      )

      SUT.sync(CREDENTIALS)

      val adopted = slot<List<SyncTombstoneEntry>>()
      coVerify(exactly = 1) { tombstoneStore.adopt(capture(adopted)) }
      assertThat(adopted.captured).contains(peerTombstone)

      val uploaded = slot<SyncPayload>()
      coVerify { remoteSource.upload(any(), capture(uploaded)) }
      assertThat(uploaded.captured.tombstones).contains(peerTombstone)
    }

  @Test
  fun `Should record the sync time only once the upload has landed`() =
    runTest {
      SUT.sync(CREDENTIALS)

      coVerifyOrder {
        remoteSource.upload(any(), any())
        settingsSyncRepository.lastSyncedAt = any()
      }
    }

  @Test
  fun `Should leave the baseline alone when the upload fails`() =
    runTest {
      // Moving it anyway would lose the deletions the failed file was carrying: the next diff would no longer re-derive them.
      coEvery { remoteSource.upload(any(), any()) } returns Result.failure(RuntimeException("offline"))

      val error = runCatching { SUT.sync(CREDENTIALS) }

      assertThat(error.isFailure).isTrue()
      coVerify(exactly = 0) { settingsSyncRepository.lastSyncedAt = any() }
    }

  @Test
  fun `Should record why a failed sync failed`() =
    runTest {
      coEvery { remoteSource.upload(any(), any()) } returns Result.failure(RuntimeException("offline"))

      runCatching { SUT.sync(CREDENTIALS) }

      coVerify(exactly = 1) { settingsSyncRepository.lastError = "offline" }
    }

  @Test
  fun `Should clear the recorded error once a sync succeeds`() =
    runTest {
      SUT.sync(CREDENTIALS)

      coVerify(exactly = 1) { settingsSyncRepository.lastError = null }
    }

  @Test
  fun `Should report the peers it merged against`() =
    runTest {
      coEvery { remoteSource.downloadPeers(any(), any()) } returns listOf(
        SyncPayload(deviceId = "tablet", updatedAt = 1, state = scheme()),
        SyncPayload(deviceId = "laptop", updatedAt = 1, state = scheme()),
      )

      val result = SUT.sync(CREDENTIALS)

      assertThat(result.peerIds).containsExactly("tablet", "laptop")
      assertThat(result.deviceId).isEqualTo(DEVICE)
    }

  // Fixtures

  private fun scheme(vararg shows: BackupShow) =
    BackupScheme(
      version = 3,
      platform = "android",
      createdAt = DATE,
      shows = BackupShows(collectionHistory = shows.toList()),
    )

  private fun show(id: Long) = BackupShow(tmdbId = id, title = "Show $id", addedAt = DATE, updatedAt = DATE)

  private fun payload(state: BackupScheme) = SyncPayload(deviceId = DEVICE, updatedAt = PREVIOUS_SYNC, state = state)

  private companion object {
    const val DEVICE = "phone"
    const val DATE = "2026-01-01T00:00:00.000Z"
    const val PREVIOUS_SYNC = 1_700_000_000_000L
    val CREDENTIALS = WebDavCredentials("https://example.com/backups/", "user", "pass")
  }
}
