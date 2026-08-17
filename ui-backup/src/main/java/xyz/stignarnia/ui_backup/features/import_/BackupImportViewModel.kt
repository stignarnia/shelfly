package xyz.stignarnia.ui_backup.features.import_

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import xyz.stignarnia.data_webdav.WebDavClient
import xyz.stignarnia.data_webdav.WebDavCredentials
import xyz.stignarnia.data_webdav.WebDavFile
import xyz.stignarnia.repository.settings.SettingsWebDavRepository
import xyz.stignarnia.ui_backup.BackupConfig.SCHEME_VERSION
import xyz.stignarnia.ui_backup.features.export.BackupFileName
import xyz.stignarnia.ui_backup.features.import_.migrations.BackupMigrationResult
import xyz.stignarnia.ui_backup.features.import_.migrations.BackupMigrationV2
import xyz.stignarnia.ui_backup.features.import_.model.BackupImportStatus.Idle
import xyz.stignarnia.ui_backup.features.import_.model.BackupImportStatus.Initializing
import xyz.stignarnia.ui_backup.features.import_.model.WebDavBackups
import xyz.stignarnia.ui_backup.features.import_.workers.BackupImportWorker
import xyz.stignarnia.ui_backup.model.BackupScheme
import xyz.stignarnia.ui_base.utilities.extensions.SUBSCRIBE_STOP_TIMEOUT
import xyz.stignarnia.ui_base.utilities.extensions.rethrowCancellation
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class BackupImportViewModel @Inject constructor(
  private val backupImportWorker: BackupImportWorker,
  private val backupMigrationV2: BackupMigrationV2,
  private val webDavRepository: SettingsWebDavRepository,
  private val webDavClient: WebDavClient,
) : ViewModel() {

  private val initialState = BackupImportUiState()

  private val importingState = MutableStateFlow(initialState.isImporting)
  private val successState = MutableStateFlow(initialState.isSuccess)
  private val errorState = MutableStateFlow(initialState.isError)
  private val reportState = MutableStateFlow(initialState.report)
  private val webDavBackupsState = MutableStateFlow<WebDavBackups>(WebDavBackups.Idle)

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
      webDavClient
        .list(credentials())
        .onSuccess { files ->
          val backups = files
            .filter { it.name.startsWith(BackupFileName.prefix) || it.name.startsWith(BackupFileName.legacyPrefix) }
            .sortedWith(compareByDescending<WebDavFile> { it.lastModifiedMillis }.thenByDescending { it.name })
            .map { it.name }
          webDavBackupsState.update { WebDavBackups.Loaded(backups) }
        }.onFailure { error ->
          webDavBackupsState.update { WebDavBackups.Idle }
          errorState.update { error }
        }
    }
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
          backupImportWorker.run(importData.scheme)
          reportState.update { importData.report }
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
      val version = jsonInput
        .substringAfter("version\":")
        .substringBefore(",")
        .trim()
        .toInt()

      return when (version) {
        SCHEME_VERSION -> {
          val moshi = Moshi
            .Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
          val scheme = moshi.adapter(BackupScheme::class.java).fromJson(jsonInput)
            ?: throw IllegalArgumentException("Backup file is empty.")
          BackupMigrationResult(scheme)
        }
        // Older schemes identify entries by ids from the previous catalog source, which this fork cannot resolve.
        // Reading one goes through an explicit migration rather than being parsed as the current scheme.
        BackupMigrationV2.VERSION -> {
          backupMigrationV2.migrate(jsonInput)
        }
        else -> {
          errorState.update { Error("Backup scheme v$version is not supported.") }
          null
        }
      }
    } catch (error: Throwable) {
      rethrowCancellation(error) {
        errorState.update { Error("Invalid backup file.\n${error.localizedMessage}") }
      }
      return null
    }
  }

  fun clearState() {
    importingState.update { Idle }
    successState.update { false }
    errorState.update { null }
    reportState.update { null }
    webDavBackupsState.update { WebDavBackups.Idle }
  }

  val uiState = combine(
    importingState,
    successState,
    errorState,
    reportState,
    webDavBackupsState,
  ) { s1, s2, s3, s4, s5 ->
    BackupImportUiState(
      isImporting = s1,
      isSuccess = s2,
      isError = s3,
      report = s4,
      webDavBackups = s5,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(SUBSCRIBE_STOP_TIMEOUT),
    initialValue = BackupImportUiState(),
  )
}
