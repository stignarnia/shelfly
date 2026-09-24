package xyz.stignarnia.uiBackup.features.imports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.stignarnia.common.extensions.dateFromMillis
import xyz.stignarnia.common.extensions.toLocalZone
import xyz.stignarnia.dataWebdav.WebDavClient
import xyz.stignarnia.dataWebdav.WebDavCredentials
import xyz.stignarnia.dataWebdav.WebDavFile
import xyz.stignarnia.repository.settings.SettingsWebDavRepository
import xyz.stignarnia.uiBackup.BackupConfig.SCHEME_VERSION
import xyz.stignarnia.uiBackup.BackupException
import xyz.stignarnia.uiBackup.R
import xyz.stignarnia.uiBackup.features.export.BackupFileName
import xyz.stignarnia.uiBackup.features.imports.migrations.BackupMigrationResult
import xyz.stignarnia.uiBackup.features.imports.migrations.BackupMigrationV2
import xyz.stignarnia.uiBackup.features.imports.model.BackupImportResult
import xyz.stignarnia.uiBackup.features.imports.model.BackupImportStatus.Idle
import xyz.stignarnia.uiBackup.features.imports.model.BackupImportStatus.Initializing
import xyz.stignarnia.uiBackup.features.imports.model.WebDavBackup
import xyz.stignarnia.uiBackup.features.imports.model.WebDavBackups
import xyz.stignarnia.uiBackup.features.imports.result.BackupImportResultHolder
import xyz.stignarnia.uiBackup.features.imports.workers.BackupImportWorker
import xyz.stignarnia.uiBackup.model.BackupScheme
import xyz.stignarnia.uiBase.dates.DateFormatProvider
import xyz.stignarnia.uiBase.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.uiBase.utilities.extensions.capitalizeWords
import xyz.stignarnia.uiBase.utilities.extensions.rethrowCancellation
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class BackupImportViewModel
  @Inject
  constructor(
    private val backupImportWorker: BackupImportWorker,
    private val backupMigrationV2: BackupMigrationV2,
    private val webDavRepository: SettingsWebDavRepository,
    private val webDavClient: WebDavClient,
    private val backupImportResultHolder: BackupImportResultHolder,
    private val dateFormatProvider: DateFormatProvider,
  ) : ViewModel() {
    private val initialState =
      BackupImportUiState(hasLastReport = backupImportResultHolder.hasReport())

    private val importingState = MutableStateFlow(initialState.isImporting)
    private val successState = MutableStateFlow(initialState.isSuccess)
    private val errorState = MutableStateFlow(initialState.isError)
    private val webDavBackupsState = MutableStateFlow<WebDavBackups>(WebDavBackups.Idle)
    private val hasLastReportState = MutableStateFlow(initialState.hasLastReport)

    init {
      backupImportWorker.statusListener = { status ->
        importingState.update { status }
        Timber.d("Importing state: $status")
      }
    }

    /** Whether a server is configured, which is what gates the WebDAV import option. */
    fun isWebDavConfigured() = webDavRepository.url.isNotBlank()

    private fun credentials() =
      WebDavCredentials(
        url = webDavRepository.url,
        username = webDavRepository.username,
        password = webDavRepository.password,
      )

    /**
     * Lists the backups on the server, newest first, so the user picks which one to restore rather than always getting the latest.
     */
    fun loadWebDavBackups() {
      if (importingState.value != Idle) return
      viewModelScope.launch {
        webDavBackupsState.update { WebDavBackups.Loading }
        listWebDavBackups()
          .onSuccess { files ->
            val dateFormat = dateFormatProvider.loadFullHourFormat()
            val backups = files.map { WebDavBackup(fileName = it.name, label = describeBackup(it, dateFormat)) }
            webDavBackupsState.update { WebDavBackups.Loaded(backups) }
          }.onFailure { error ->
            webDavBackupsState.update { WebDavBackups.Idle }
            errorState.update { error }
          }
      }
    }

    /**
     * Restores the newest backup on the server without asking which one.
     * This is the empty home screen's sync button, where there is no library yet for an older backup to be preferred over.
     */
    fun importLatestWebDavBackup() {
      if (importingState.value != Idle) return
      viewModelScope.launch {
        webDavBackupsState.update { WebDavBackups.Loading }
        listWebDavBackups()
          .onSuccess { files ->
            webDavBackupsState.update { WebDavBackups.Idle }
            val latest = files.firstOrNull()
            if (latest == null) {
              errorState.update { BackupException(R.string.textBackupWebDavNoBackups) }
            } else {
              runWebDavImport(latest.name)
            }
          }.onFailure { error ->
            webDavBackupsState.update { WebDavBackups.Idle }
            errorState.update { error }
          }
      }
    }

    private suspend fun listWebDavBackups() =
      webDavClient
        .list(credentials())
        .map { files ->
          files
            .filter { it.name.startsWith(BackupFileName.PREFIX) || it.name.startsWith(BackupFileName.LEGACY_PREFIX) }
            .sortedWith(compareByDescending<WebDavFile> { it.lastModifiedMillis }.thenByDescending { it.name })
        }

    /**
     * When the backup was made, in the user's date format.
     * The time stamped into the name comes first because it is when the backup was written, where the server's modification time can move when files are copied around.
     * The raw name is the last resort for a file neither can date.
     */
    private fun describeBackup(
      file: WebDavFile,
      dateFormat: DateTimeFormatter,
    ): String {
      val date =
        BackupFileName.parseDate(file.name)
          ?: file.lastModifiedMillis
            .takeIf { it > 0 }
            ?.let { dateFromMillis(it).toLocalZone().toLocalDateTime() }
          ?: return file.name
      return dateFormat.format(date).capitalizeWords()
    }

    /** Downloads the chosen backup and hands it to the same import path as a local file. */
    fun runWebDavImport(fileName: String) {
      if (importingState.value != Idle) return
      viewModelScope.launch {
        webDavBackupsState.update { WebDavBackups.Idle }
        importingState.update { Initializing }
        webDavClient
          .get(credentials(), fileName)
          .onSuccess { json ->
            importingState.update { Idle }
            runImport(json)
          }.onFailure { error ->
            importingState.update { Idle }
            errorState.update { error }
          }
      }
    }

    fun clearWebDavBackups() {
      webDavBackupsState.update { WebDavBackups.Idle }
    }

    fun runImport(jsonInput: String) {
      if (importingState.value != Idle) return
      viewModelScope.launch {
        try {
          importingState.update { Initializing }
          delay(1.seconds)
          val importData = createImportData(jsonInput)
          if (importData != null) {
            val workerResult = backupImportWorker.run(importData.scheme)
            val allUnmatchedShows =
              importData.report.unmatchedShows + workerResult.failedShows
            val allUnmatchedMovies = importData.report.unmatchedMovies + workerResult.failedMovies
            val allUnmatchedLists = importData.report.unmatchedLists + workerResult.failedLists
            val importResult =
              BackupImportResult(
                importedMoviesCount = workerResult.importedMoviesCount,
                importedShowsCount = workerResult.importedShowsCount,
                unmatchedMovies = allUnmatchedMovies,
                unmatchedShows = allUnmatchedShows,
                unmatchedLists = allUnmatchedLists,
              )
            backupImportResultHolder.result = importResult
            hasLastReportState.update { true }
            successState.update { true }
          }
        } catch (error: Throwable) {
          rethrowCancellation(error) {
            errorState.update { error }
          }
        } finally {
          importingState.update { Idle }
        }
      }
    }

    private suspend fun createImportData(jsonInput: String): BackupMigrationResult? {
      try {
        val version =
          jsonInput
            .substringAfter("version\":")
            .substringBefore(",")
            .trim()
            .toInt()

        return when (version) {
          SCHEME_VERSION -> {
            val moshi =
              Moshi
                .Builder()
                .build()
            val scheme =
              moshi.adapter(BackupScheme::class.java).fromJson(jsonInput)
                ?: throw IllegalArgumentException("Backup file is empty.")
            BackupMigrationResult(scheme)
          }

          // Older schemes identify entries by ids from the previous catalog source, which this fork cannot resolve.
          // Reading one goes through an explicit migration rather than being parsed as the current scheme.
          BackupMigrationV2.VERSION -> {
            backupMigrationV2.migrate(jsonInput)
          }

          else -> {
            errorState.update { BackupException(R.string.textBackupImportUnsupportedVersion, version) }
            null
          }
        }
      } catch (error: Throwable) {
        rethrowCancellation(error) {
          errorState.update { BackupException(R.string.textBackupImportInvalidFile, cause = error) }
        }
        return null
      }
    }

    fun clearState() {
      importingState.update { Idle }
      successState.update { false }
      errorState.update { null }
      webDavBackupsState.update { WebDavBackups.Idle }
    }

    val uiState =
      combine(
        importingState,
        successState,
        errorState,
        webDavBackupsState,
        hasLastReportState,
      ) { isImporting, isSuccess, isError, webDavBackups, hasLastReport ->
        BackupImportUiState(
          isImporting = isImporting,
          isSuccess = isSuccess,
          isError = isError,
          webDavBackups = webDavBackups,
          hasLastReport = hasLastReport,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
        initialValue = initialState,
      )
  }
