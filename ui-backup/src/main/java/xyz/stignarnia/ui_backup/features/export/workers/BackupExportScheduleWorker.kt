package xyz.stignarnia.ui_backup.features.export.workers

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.data_webdav.WebDavClient
import xyz.stignarnia.data_webdav.WebDavCredentials
import xyz.stignarnia.repository.settings.SettingsWebDavRepository
import xyz.stignarnia.ui_backup.features.export.BackupFileName
import xyz.stignarnia.ui_backup.R
import xyz.stignarnia.ui_backup.features.export.cases.CreateBackupJsonUseCase
import xyz.stignarnia.ui_backup.features.export.cases.CreateBackupSchemeFromJsonUseCase
import xyz.stignarnia.ui_backup.features.export.cases.ReadBackupJsonFromFileUseCase
import xyz.stignarnia.ui_backup.features.export.cases.WriteBackupJsonToFileUseCase
import xyz.stignarnia.ui_backup.features.export.model.BackupExportSchedule
import xyz.stignarnia.ui_backup.features.export.targets.BackupDestination
import xyz.stignarnia.ui_backup.features.export.targets.BackupEntry
import xyz.stignarnia.ui_backup.features.export.targets.LocalFolderBackupDestination
import xyz.stignarnia.ui_backup.features.export.targets.WebDavBackupDestination
import xyz.stignarnia.ui_backup.features.sync.BackupSyncStage
import xyz.stignarnia.ui_backup.features.sync.SyncEngine
import xyz.stignarnia.ui_base.notifications.SyncNotificationManager
import xyz.stignarnia.ui_model.BackupTarget
import javax.inject.Named

/**
 * Creates a backup on a schedule, on behalf of the user.
 *
 * The pipeline is the same whichever target is configured: build the JSON, write it, read it straight back and parse it to prove the write landed, then prune to the newest few.
 * Only the storage differs, which is what [BackupDestination] abstracts.
 *
 * A WebDAV target then syncs with the user's other devices.
 * The snapshot is written first and is never skipped: sync propagates an accidental deletion rather than protecting against one, so the retained copies are what makes it safe to run at all.
 * Sync needs a server both ends can see, so a local folder target only ever takes the snapshot.
 */
@HiltWorker
class BackupExportScheduleWorker @AssistedInject constructor(
  @Assisted appContext: Context,
  @Assisted workerParams: WorkerParameters,
  private val createBackupJsonUseCase: CreateBackupJsonUseCase,
  private val writeBackupJsonToFileUseCase: WriteBackupJsonToFileUseCase,
  private val readBackupJsonFromFileUseCase: ReadBackupJsonFromFileUseCase,
  private val createBackupSchemeFromJsonUseCase: CreateBackupSchemeFromJsonUseCase,
  private val webDavRepository: SettingsWebDavRepository,
  private val webDavClient: WebDavClient,
  private val syncEngine: SyncEngine,
  private val syncNotificationManager: SyncNotificationManager,
  @param:Named("miscPreferences") private val miscPreferences: SharedPreferences,
) : CoroutineWorker(appContext, workerParams) {

  companion object {
    private const val TAG = "BACKUP_EXPORT_WORK"
    const val TAG_ONE_OFF = "BACKUP_EXPORT_WORK_ONE_OFF"

    /**
     * Percentage complete, published while the work runs.
     * See [BackupSyncStage].
     */
    const val KEY_PROGRESS_PERCENT = "KEY_PROGRESS_PERCENT"

    const val KEY_BACKUP_EXPORT_SCHEDULE = "KEY_BACKUP_EXPORT_SCHEDULE"
    const val KEY_BACKUP_EXPORT_DIRECTORY_URI = "KEY_BACKUP_EXPORT_DIRECTORY_URI"
    const val KEY_LAST_LAST_BACKUP_EXPORT_TIMESTAMP = "KEY_LAST_LAST_BACKUP_EXPORT_TIMESTAMP"
    private const val ARG_DIRECTORY_URI = "ARG_DIRECTORY_URI"

    /**
     * Schedules a periodic backup export, replacing any existing schedule.
     * A schedule of [BackupExportSchedule.OFF] schedules nothing.
     *
     * @param directoryUri Folder to write into.
     * Ignored when the configured target is WebDAV, which addresses its directory by URL instead.
     */
    fun schedulePeriodic(
      workManager: WorkManager,
      directoryUri: Uri?,
      schedule: BackupExportSchedule,
      cancelExisting: Boolean,
    ) {
      if (cancelExisting) {
        cancelAllPeriodic(workManager)
      }

      if (schedule == BackupExportSchedule.OFF) {
        Timber.i("Backup export scheduled: $schedule")
        return
      }

      val data = Data
        .Builder()
        .putString(ARG_DIRECTORY_URI, directoryUri?.toString())
        .build()

      val request = PeriodicWorkRequestBuilder<BackupExportScheduleWorker>(schedule.duration, schedule.durationUnit)
        .setInputData(data)
        .setInitialDelay(schedule.duration, schedule.durationUnit)
        // A WebDAV target is useless offline, and the local target does not suffer from waiting for a connection that is coming anyway.
        .setConstraints(
          Constraints
            .Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build(),
        ).addTag(TAG)
        .build()

      workManager.enqueueUniquePeriodicWork(
        uniqueWorkName = TAG,
        existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP,
        request = request,
      )

      Timber.i("Backup export scheduled: $schedule")
    }

    /**
     * Runs a backup once, now, against whichever destination is configured.
     * Used by the pull-to-backup gesture on the progress screens.
     *
     * Enqueued as unique work that keeps any run already in flight, so an impatient second pull does not start a duplicate backup.
     */
    fun scheduleOneOff(workManager: WorkManager) {
      val request = OneTimeWorkRequestBuilder<BackupExportScheduleWorker>()
        .setConstraints(
          Constraints
            .Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build(),
        ).addTag(TAG_ONE_OFF)
        .build()

      workManager.enqueueUniqueWork(
        uniqueWorkName = TAG_ONE_OFF,
        existingWorkPolicy = ExistingWorkPolicy.KEEP,
        request = request,
      )
    }

    /**
     * Cancel all scheduled work.
     */
    fun cancelAllPeriodic(workManager: WorkManager) {
      workManager.cancelUniqueWork(TAG)
    }
  }

  /**
   * Creating a backup matters more than tidying old ones away, so a failed prune is logged and still reports success.
   */
  override suspend fun doWork(): Result {
    Timber.i("Exporting automatic backup")

    val isWebDav = webDavRepository.backupTarget == BackupTarget.WEBDAV
    if (isWebDav) {
      syncNotificationManager.showProgress(
        title = applicationContext.getString(R.string.textSyncNotificationProgressTitle),
        message = applicationContext.getString(R.string.textSyncNotificationProgressContent),
      )
    }

    try {
      val destination = try {
        resolveDestination().also { report(BackupSyncStage.DESTINATION_RESOLVED) }
      } catch (exception: Exception) {
        Timber.w(exception, "Backup destination is not configured")
        if (isWebDav) {
          syncNotificationManager.showError(
            title = applicationContext.getString(R.string.textBackupNotificationErrorTitle),
            message = exception.localizedMessage ?: exception.message ?: "Backup destination is not configured",
          )
        }
        return Result.failure()
      }

      try {
        exportNewBackup(destination)
        Timber.i("Exporting automatic backup successful")
      } catch (exception: Exception) {
        Timber.w(exception, "Exporting automatic backup failed")
        if (isWebDav) {
          syncNotificationManager.showError(
            title = applicationContext.getString(R.string.textBackupNotificationErrorTitle),
            message = exception.localizedMessage ?: exception.message ?: "Exporting automatic backup failed",
          )
        }
        return Result.failure()
      }

      try {
        pruneOldBackups(destination)
        Timber.i("Cleaning up old backups successful")
      } catch (exception: Exception) {
        Timber.w(exception, "Cleaning up of old backups failed")
      }
      // Reported either way: a failed prune still reports success, so the stage is behind us regardless of which branch got here.
      report(BackupSyncStage.OLD_BACKUPS_PRUNED)

      val credentials = webDavCredentials()
      if (credentials != null) {
        try {
          val result = syncEngine.sync(credentials, onStage = ::report)
          Timber.i("Sync successful against ${result.peers} peer(s)")
          syncNotificationManager.cancelError()
          syncNotificationManager.showSuccess(
            title = applicationContext.getString(R.string.textSyncNotificationSuccessTitle),
            message = applicationContext.getString(R.string.textSyncNotificationSuccessContent),
          )
        } catch (exception: Exception) {
          // Reported rather than swallowed: the snapshot above is safe either way, but a device that quietly stops syncing drifts from the others and nothing says so.
          Timber.w(exception, "Sync failed")
          val errorMsg = exception.localizedMessage ?: exception.message ?: "Sync failed"
          syncNotificationManager.showError(
            title = applicationContext.getString(R.string.textSyncNotificationErrorTitle),
            message = applicationContext.getString(R.string.textSyncNotificationErrorContent, errorMsg),
          )
          return Result.failure()
        }
      }

      return Result.success()
    } finally {
      if (isWebDav) {
        syncNotificationManager.cancelProgress()
      }
    }
  }

  /** Configured WebDAV credentials, or null when the target is a local folder. */
  private fun webDavCredentials(): WebDavCredentials? {
    if (webDavRepository.backupTarget != BackupTarget.WEBDAV) {
      return null
    }
    return WebDavCredentials(
      url = webDavRepository.url,
      username = webDavRepository.username,
      password = webDavRepository.password,
    ).takeIf { it.isComplete }
  }

  private fun resolveDestination(): BackupDestination =
    when (webDavRepository.backupTarget) {
      BackupTarget.WEBDAV -> {
        val credentials = webDavCredentials()
        require(credentials != null) { "WebDAV backup is selected but no server is configured." }
        WebDavBackupDestination(webDavClient, credentials)
      }

      BackupTarget.LOCAL_FOLDER -> {
        val directoryUri = inputData.getString(ARG_DIRECTORY_URI)?.toUri()
          ?: miscPreferences.getString(KEY_BACKUP_EXPORT_DIRECTORY_URI, null)?.toUri()
          ?: throw IllegalArgumentException("Directory URI is null")
        LocalFolderBackupDestination(
          context = applicationContext,
          directoryUri = directoryUri,
          writeBackupJsonToFileUseCase = writeBackupJsonToFileUseCase,
          readBackupJsonFromFileUseCase = readBackupJsonFromFileUseCase,
        )
      }
    }

  /**
   * Writes a backup and proves it landed by reading it back and parsing it.
   * The timestamp is only recorded once that has succeeded, so a failed write cannot masquerade as a recent backup.
   */
  private suspend fun exportNewBackup(destination: BackupDestination) {
    val fileName = BackupFileName.create()
    val backupJson = createBackupJsonUseCase()

    destination.write(fileName, backupJson).getOrThrow()
    report(BackupSyncStage.SNAPSHOT_WRITTEN)

    val writtenJson = destination.read(fileName).getOrThrow()
    createBackupSchemeFromJsonUseCase(writtenJson).getOrThrow()
    report(BackupSyncStage.SNAPSHOT_VERIFIED)

    miscPreferences.edit { putLong(KEY_LAST_LAST_BACKUP_EXPORT_TIMESTAMP, nowUtcMillis()) }
  }

  /**
   * Publishes how far along the run is, for the pull gesture that started it.
   *
   * WorkManager keeps this only while the work is RUNNING and drops it when the run ends, so an observer needs no "finished" stage - the progress going away IS the run being over, whether it succeeded or failed.
   */
  private suspend fun report(stage: BackupSyncStage) {
    setProgress(workDataOf(KEY_PROGRESS_PERCENT to stage.percent))
  }

  /**
   * Keeps the newest backups and deletes the rest, according to the retention the user configured.
   * A retention of [SettingsWebDavRepository.RETENTION_KEEP_ALL] deletes nothing at all.
   *
   * Sorted by modification time, falling back to the name - which carries a sortable timestamp - because not every WebDAV server reports a `getlastmodified`, and entries without one would otherwise all look equally old and be deleted arbitrarily.
   */
  private suspend fun pruneOldBackups(destination: BackupDestination) {
    val keep = webDavRepository.backupRetention
    if (keep <= SettingsWebDavRepository.RETENTION_KEEP_ALL) {
      Timber.i("Retention is set to keep all backups. Nothing to prune.")
      return
    }

    val backups = destination
      .list()
      .getOrThrow()
      .filter { it.isBackup() }
      .sortedWith(compareBy({ it.lastModifiedMillis }, { it.name }))

    if (backups.size <= keep) return

    backups.take(backups.size - keep).forEach { entry ->
      destination.delete(entry).onFailure {
        Timber.w(it, "Failed to delete old backup: ${entry.name}")
      }
    }
  }

  /** Recognises backups written before the rename too, so they are pruned rather than left to pile up. */
  private fun BackupEntry.isBackup(): Boolean =
    (name.startsWith(BackupFileName.prefix) || name.startsWith(BackupFileName.legacyPrefix)) &&
      name.endsWith(BackupFileName.fileType)
}
