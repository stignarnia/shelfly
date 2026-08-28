package xyz.stignarnia.shelfly.ui.main.cases

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.work.WorkManager
import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.repository.settings.SettingsWebDavRepository
import xyz.stignarnia.uiBackup.features.export.model.BackupExportSchedule
import xyz.stignarnia.uiBackup.features.export.workers.BackupExportScheduleWorker
import xyz.stignarnia.uiModel.BackupTarget
import javax.inject.Inject
import javax.inject.Named

@ViewModelScoped
class MainBackupCase
  @Inject
  constructor(
    @param:Named("miscPreferences") private var miscPreferences: SharedPreferences,
    private val webDavRepository: SettingsWebDavRepository,
    private val workManager: WorkManager,
  ) {
    /**
     * Re-arms the periodic backup on app start, so a schedule survives the system dropping the work.
     *
     * Whether a schedule is still valid depends on the configured destination.
     * A local folder needs its picked directory; a WebDAV server needs a URL and has no directory at all.
     * Checking only for the directory would silently cancel every WebDAV schedule on the next launch.
     */
    fun refreshBackupExportSchedule() {
      val schedule =
        BackupExportSchedule.createFromName(
          miscPreferences.getString(BackupExportScheduleWorker.KEY_BACKUP_EXPORT_SCHEDULE, null),
        )
      val directoryUri =
        miscPreferences
          .getString(BackupExportScheduleWorker.KEY_BACKUP_EXPORT_DIRECTORY_URI, null)
          ?.toUri()

      val isDestinationUsable =
        when (webDavRepository.backupTarget) {
          BackupTarget.WEBDAV -> webDavRepository.url.isNotBlank()
          BackupTarget.LOCAL_FOLDER -> directoryUri != null
        }

      if (isDestinationUsable) {
        BackupExportScheduleWorker.schedulePeriodic(
          workManager = workManager,
          directoryUri = directoryUri,
          schedule = schedule,
          cancelExisting = false,
        )
      } else {
        miscPreferences.edit {
          putString(
            BackupExportScheduleWorker.KEY_BACKUP_EXPORT_SCHEDULE,
            BackupExportSchedule.DEFAULT_OFF.name,
          )
        }
        BackupExportScheduleWorker.cancelAllPeriodic(workManager)
      }
    }
  }
