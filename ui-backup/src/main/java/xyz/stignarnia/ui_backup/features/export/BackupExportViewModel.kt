package xyz.stignarnia.ui_backup.features.export

import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import xyz.stignarnia.common.extensions.nowUtcMillis
import xyz.stignarnia.ui_backup.features.export.cases.CreateBackupJsonUseCase
import xyz.stignarnia.ui_backup.features.export.cases.CreateBackupSchemeFromJsonUseCase
import xyz.stignarnia.ui_backup.features.export.model.BackupExportSchedule
import xyz.stignarnia.ui_backup.features.export.workers.BackupExportScheduleWorker
import xyz.stignarnia.ui_backup.model.BackupScheme
import xyz.stignarnia.ui_model.BackupTarget
import xyz.stignarnia.repository.settings.SettingsSyncRepository
import xyz.stignarnia.repository.settings.SettingsWebDavRepository
import xyz.stignarnia.ui_base.dates.DateFormatProvider
import xyz.stignarnia.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.ui_base.utilities.extensions.combine
import xyz.stignarnia.ui_base.utilities.extensions.rethrowCancellation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class BackupExportViewModel @Inject constructor(
  @param:Named("miscPreferences") private var miscPreferences: SharedPreferences,
  private val createBackupJsonUseCase: CreateBackupJsonUseCase,
  private val createBackupSchemeFromJsonUseCase: CreateBackupSchemeFromJsonUseCase,
  private val workManager: WorkManager,
  private val webDavRepository: SettingsWebDavRepository,
  private val syncRepository: SettingsSyncRepository,
  dateFormatProvider: DateFormatProvider,
) : ViewModel() {

  private val initialState = BackupExportUiState(
    backupExportSchedule = BackupExportSchedule.createFromName(
      miscPreferences.getString(BackupExportScheduleWorker.KEY_BACKUP_EXPORT_SCHEDULE, null),
    ),
    lastBackupExportTimestamp =
      miscPreferences.getLong(BackupExportScheduleWorker.KEY_LAST_LAST_BACKUP_EXPORT_TIMESTAMP, 0),
    syncStatus = null,
    dateFormat = dateFormatProvider.loadFullHourFormat(),
  )

  private val exportContentState = MutableStateFlow(initialState.exportContent)
  private val loadingState = MutableStateFlow(initialState.isLoading)
  private val errorState = MutableStateFlow(initialState.error)
  private val backupExportScheduleState = MutableStateFlow(initialState.backupExportSchedule)
  private val lastBackupExportTimestampState = MutableStateFlow(initialState.lastBackupExportTimestamp)
  private val syncStatusState = MutableStateFlow(initialState.syncStatus)
  private val dateFormatState = MutableStateFlow(initialState.dateFormat)

  /**
   * Run on-off export.
   */
  fun runOneOffExport(uri: Uri) {
    if (loadingState.value) return
    viewModelScope.launch {
      try {
        loadingState.update { true }
        val exportJson = createBackupJsonUseCase()
        exportContentState.update {
          ExportContentState(
            exportContent = exportJson,
            exportUri = uri,
          )
        }
      } catch (error: Throwable) {
        rethrowCancellation(error) {
          errorState.update { error }
        }
      } finally {
        loadingState.update { false }
      }
    }
  }

  /**
   * Validates the provided JSON input string and attempts to create a [BackupScheme] from it.
   *
   * @param jsonInput The JSON string to validate.
   * @return A [Result] object containing the parsed [BackupScheme] on success, or an [Error] on failure.
   */
  fun validateExportData(jsonInput: String): Result<BackupScheme> {
    val jsonError = Error("Failed to validate export file. Please try again or contact us if this keeps happening.")
    return createBackupSchemeFromJsonUseCase(jsonInput).fold(
      onSuccess = {
        if (it != null) {
          Result.success(it)
        } else {
          Result.failure(jsonError)
        }
      },
      onFailure = { exception ->
        rethrowCancellation(exception) {
          errorState.update { jsonError }
        }
        return Result.failure(jsonError)
      },
    )
  }

  /**
   * Called when export validation is successful.
   * Updates the last backup export timestamp in shared preferences and in the UI state.
   */
  fun onExportValidationSuccess() {
    val now = nowUtcMillis()
    miscPreferences.edit { putLong(BackupExportScheduleWorker.KEY_LAST_LAST_BACKUP_EXPORT_TIMESTAMP, now) }
    lastBackupExportTimestampState.update { now }
  }

  /** Whether scheduled backups go to WebDAV, in which case no folder is needed. */
  fun isWebDavTarget() = webDavRepository.backupTarget == BackupTarget.WEBDAV

  /**
   * Re-reads how syncing is going.
   * Called on every resume because the work runs in the background, so the screen would otherwise show whatever was true when it was opened.
   */
  fun refreshSyncStatus() {
    if (!isWebDavTarget()) {
      syncStatusState.value = null
      return
    }
    syncStatusState.value = SyncStatus(
      deviceId = syncRepository.deviceId,
      lastSyncedAt = syncRepository.lastSyncedAt,
      peers = syncRepository.lastPeers,
      error = syncRepository.lastError,
    )
  }

  /**
   * Set up an automatic export schedule against the WebDAV server.
   * The destination is the configured URL, so unlike the local folder there is nothing to pick.
   */
  fun saveExportBackupSchedule(schedule: BackupExportSchedule) {
    viewModelScope.launch {
      miscPreferences.edit { putString(BackupExportScheduleWorker.KEY_BACKUP_EXPORT_SCHEDULE, schedule.name) }
      BackupExportScheduleWorker.schedulePeriodic(
        workManager = workManager,
        directoryUri = null,
        schedule = schedule,
        cancelExisting = true,
      )
      backupExportScheduleState.value = schedule
    }
  }

  /**
   * Set up automatic export schedule.
   */
  fun saveExportBackupSchedule(
    directoryUri: Uri,
    schedule: BackupExportSchedule,
  ) {
    viewModelScope.launch {
      miscPreferences.edit { putString(BackupExportScheduleWorker.KEY_BACKUP_EXPORT_SCHEDULE, schedule.name) }
      miscPreferences.edit {
        putString(
          BackupExportScheduleWorker.KEY_BACKUP_EXPORT_DIRECTORY_URI,
          directoryUri.toString(),
        )
      }
      BackupExportScheduleWorker.schedulePeriodic(
        workManager = workManager,
        directoryUri = directoryUri,
        schedule = schedule,
        cancelExisting = true,
      )
      backupExportScheduleState.value = schedule
    }
  }

  /**
   * Set schedule to OFF.
   * This cancels ongoing schedules.
   */
  fun saveExportBackupScheduleOff() {
    val offSchedule = BackupExportSchedule.OFF
    viewModelScope.launch {
      miscPreferences.edit { putString(BackupExportScheduleWorker.KEY_BACKUP_EXPORT_SCHEDULE, offSchedule.name) }
      miscPreferences.edit { remove(BackupExportScheduleWorker.KEY_BACKUP_EXPORT_DIRECTORY_URI) }
      BackupExportScheduleWorker.cancelAllPeriodic(workManager)
      backupExportScheduleState.value = offSchedule
    }
  }

  fun clearOneOffState() {
    loadingState.update { false }
    exportContentState.update { null }
    errorState.update { null }
  }

  val uiState = combine(
    loadingState,
    exportContentState,
    errorState,
    backupExportScheduleState,
    lastBackupExportTimestampState,
    syncStatusState,
    dateFormatState,
  ) { s1, s2, s3, s4, s5, s6, s7 ->
    BackupExportUiState(
      isLoading = s1,
      exportContent = s2,
      error = s3,
      backupExportSchedule = s4,
      lastBackupExportTimestamp = s5,
      syncStatus = s6,
      dateFormat = s7,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
    initialValue = BackupExportUiState(),
  )
}
