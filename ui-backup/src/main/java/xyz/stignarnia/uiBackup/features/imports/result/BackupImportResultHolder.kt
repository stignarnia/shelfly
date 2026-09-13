package xyz.stignarnia.uiBackup.features.imports.result

import android.content.Context
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import xyz.stignarnia.uiBackup.features.imports.model.BackupImportResult
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupImportResultHolder
  @Inject
  constructor(
    @ApplicationContext private val context: Context,
  ) {
    private val moshi: Moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(BackupImportResult::class.java)

    private val reportFile: File
      get() = File(context.filesDir, REPORT_FILE_NAME)

    private var cachedResult: BackupImportResult? = null
    private var isLoaded = false

    var result: BackupImportResult?
      get() {
        if (!isLoaded) {
          cachedResult = loadReport()
          isLoaded = true
        }
        return cachedResult
      }
      set(value) {
        cachedResult = value
        isLoaded = true
        if (value != null) {
          saveReport(value)
        }
      }

    fun hasReport(): Boolean = result != null

    private fun loadReport(): BackupImportResult? =
      try {
        if (reportFile.exists() && reportFile.length() > 0) {
          val json = reportFile.readText()
          adapter.fromJson(json)
        } else {
          null
        }
      } catch (e: Throwable) {
        Timber.e(e, "Failed to load last import report")
        null
      }

    private fun saveReport(report: BackupImportResult) {
      try {
        val json = adapter.toJson(report)
        reportFile.writeText(json)
      } catch (e: Throwable) {
        Timber.e(e, "Failed to save last import report")
      }
    }

    companion object {
      private const val REPORT_FILE_NAME = "last_import_report.json"
    }
  }
