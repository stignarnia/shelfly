package xyz.stignarnia.uiBackup.features.export.cases

import com.squareup.moshi.Moshi
import xyz.stignarnia.uiBackup.features.export.workers.BackupExportWorker
import xyz.stignarnia.uiBackup.model.BackupScheme
import javax.inject.Inject

/**
 * Creates a JSON backup string.
 */
class CreateBackupJsonUseCase
  @Inject
  constructor(
    private val backupExportWorker: BackupExportWorker,
  ) {
    /**
     * Creates a JSON backup string.
     */
    suspend operator fun invoke(): String {
      val exportResult = backupExportWorker.run()
      return createExportJson(exportResult)
    }

    private fun createExportJson(exportContent: BackupScheme): String {
      val moshi =
        Moshi
          .Builder()
          .build()
      val jsonAdapter = moshi.adapter(BackupScheme::class.java)
      return jsonAdapter.toJson(exportContent)
    }
  }
