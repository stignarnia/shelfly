package xyz.stignarnia.ui_backup.features.export

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import xyz.stignarnia.common.extensions.dateFromMillis
import xyz.stignarnia.common.extensions.toLocalZone
import xyz.stignarnia.ui_backup.R
import xyz.stignarnia.ui_backup.databinding.FragmentBackupExportBinding
import xyz.stignarnia.ui_backup.features.export.cases.ReadBackupJsonFromFileUseCase
import xyz.stignarnia.ui_backup.features.export.cases.WriteBackupJsonToFileUseCase
import xyz.stignarnia.ui_backup.features.export.model.BackupExportSchedule
import xyz.stignarnia.ui_base.BaseFragment
import xyz.stignarnia.ui_base.utilities.SnackbarHost
import xyz.stignarnia.ui_base.utilities.events.MessageEvent
import xyz.stignarnia.ui_base.utilities.extensions.capitalizeWords
import xyz.stignarnia.ui_base.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.ui_base.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.showErrorSnackbar
import xyz.stignarnia.ui_base.utilities.extensions.showInfoSnackbar
import xyz.stignarnia.ui_base.utilities.extensions.visibleIf
import xyz.stignarnia.ui_base.utilities.viewBinding
import java.time.format.DateTimeFormatter
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class BackupExportFragment : BaseFragment<BackupExportViewModel>(R.layout.fragment_backup_export) {

  @Inject
  lateinit var writeBackupJsonToFileUseCase: WriteBackupJsonToFileUseCase

  @Inject
  lateinit var readBackupJsonFromFileUseCase: ReadBackupJsonFromFileUseCase

  override val viewModel by viewModels<BackupExportViewModel>()
  private val binding by viewBinding(FragmentBackupExportBinding::bind)

  private var selectedSchedule: BackupExportSchedule? = null

  private val createFileContract =
    registerForActivityResult(CreateDocument("application/json")) { uri ->
      uri?.let {
        viewModel.runOneOffExport(uri)
      }
    }

  /**
   * For automatic backups we need access to a whole folder rather then just one file.
   */
  private val createFolderContract =
    registerForActivityResult(OpenDocumentTree()) { directoryUri ->
      directoryUri?.let {
        // Take persistable permissions so you can use this URI across app restarts.
        val permissionsFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        requireActivity().contentResolver.takePersistableUriPermission(directoryUri, permissionsFlags)
        selectedSchedule?.let {
          selectedSchedule = null
          viewModel.saveExportBackupSchedule(directoryUri, it)
          showSnack(MessageEvent.Info(it.confirmationStringRes))
        }
      }
    }

  private var snackbar: Snackbar? = null

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    setupInsets()

    launchAndRepeatStarted(
      { viewModel.uiState.collect { render(it) } },
      doAfterLaunch = { viewModel.refreshSyncStatus() },
    )
  }

  override fun onResume() {
    super.onResume()
    // Sync runs in the background, so what was true when this screen opened may
    // not be true now.
    viewModel.refreshSyncStatus()
  }

  private fun setupView() {
    with(binding) {
      toolbar.onClick { activity?.onBackPressed() }
      exportButton.onClick { createNewExport() }
    }
  }

  private fun setupInsets() {
    with(binding) {
      root.doOnApplyWindowInsets { view, insets, padding, _ ->
        val inset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(
          top = padding.top + inset.top,
          bottom = padding.bottom + inset.bottom,
        )
      }
    }
  }

  private fun createNewExport() {
    val fileName = BackupFileName.create()
    createFileContract.launch(fileName)
  }

  private fun saveNewExport(
    uri: Uri,
    content: String,
  ) {
    writeBackupJsonToFileUseCase(requireContext(), uri, content).fold(
      onSuccess = { /* NO-OP */ },
      onFailure = {
        showErrorSnack(it)
        Timber.e(it)
      },
    )
  }

  private fun validateExportFile(uri: Uri) {
    readBackupJsonFromFileUseCase(requireContext(), uri).fold(
      onSuccess = { json ->
        viewModel
          .validateExportData(json)
          .onSuccess {
            viewModel.onExportValidationSuccess()
            showShareSnack(uri)
          }.onFailure {
            showErrorSnack(it)
            Timber.e(it)
          }
      },
      onFailure = {
        showErrorSnack(it)
        Timber.e(it)
      },
    )
  }

  private fun shareNewExport(uri: Uri) {
    val intent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_STREAM, uri)
      type = "application/json"
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context?.startActivity(Intent.createChooser(intent, "Share"))
  }

  private fun showShareSnack(uri: Uri) {
    val host = (requireActivity() as SnackbarHost).provideSnackbarLayout()
    snackbar = host.showInfoSnackbar(
      message = getString(R.string.textBackupExportSuccess),
      actionText = R.string.textShare,
      length = 10.seconds.inWholeMilliseconds.toInt(),
      action = { shareNewExport(uri) },
    )
  }

  private fun showErrorSnack(error: Throwable) {
    val host = (requireActivity() as SnackbarHost).provideSnackbarLayout()
    snackbar = host.showErrorSnackbar(
      message = error.localizedMessage ?: getString(R.string.errorGeneral),
    )
  }

  override fun onDestroyView() {
    snackbar?.dismiss()
    super.onDestroyView()
  }

  private fun render(uiState: BackupExportUiState) {
    uiState.run {
      with(binding) {
        progressBar.visibleIf(isLoading)
        statusText.visibleIf(isLoading)
        exportButton.visibleIf(!isLoading, gone = false)
        exportButton.isEnabled = !isLoading
        exportScheduleButton.visibleIf(!isLoading, gone = false)
        exportScheduleButton.isEnabled = !isLoading
        exportScheduleButton.setText(backupExportSchedule.buttonStringRes)
        exportScheduleButton.onClick { showScheduleDialog(backupExportSchedule) }
        lastExportTimestamp.visibleIf(!isLoading && lastBackupExportTimestamp != 0L)
        if (lastBackupExportTimestamp != 0L) {
          val date = dateFormat?.format(dateFromMillis(lastBackupExportTimestamp).toLocalZone())?.capitalizeWords()
          lastExportTimestamp.text = getString(R.string.textBackupExportLastTimestamp, date)
        }
        syncStatus.visibleIf(!isLoading && uiState.syncStatus != null)
        uiState.syncStatus?.let { syncStatus.text = describeSync(it, dateFormat) }
      }
      exportContent?.let {
        saveNewExport(it.exportUri, it.exportContent)
        validateExportFile(it.exportUri)
        viewModel.clearOneOffState()
      }
      if (error != null) {
        showErrorSnack(error)
        viewModel.clearOneOffState()
      }
    }
  }

  /**
   * Says what syncing has actually been doing, failures included.
   *
   * A failure is shown next to the last success rather than replacing it: how
   * long ago the last good sync was is exactly what the user needs in order to
   * judge how much a broken one matters.
   */
  private fun describeSync(
    status: SyncStatus,
    dateFormat: DateTimeFormatter?,
  ): String {
    val lastSync = when (status.lastSyncedAt) {
      0L -> {
        getString(R.string.textSyncNever)
      }
      else -> {
        val date = dateFormat?.format(dateFromMillis(status.lastSyncedAt).toLocalZone())?.capitalizeWords()
        val peers = resources.getQuantityString(R.plurals.textSyncPeers, status.peers.size, status.peers.size)
        "${getString(R.string.textSyncLastTimestamp, date)} \u00b7 $peers"
      }
    }
    return when (val error = status.error) {
      null -> lastSync
      else -> "$lastSync\n${getString(R.string.textSyncFailed, error)}"
    }
  }

  /**
   * Displays a dialog to select the backup export schedule.
   *
   * @param currentSchedule The currently selected backup export schedule.
   */
  private fun showScheduleDialog(currentSchedule: BackupExportSchedule) =
    showSingleChoiceDialog(
      options = BackupExportSchedule.entries,
      selected = currentSchedule,
      label = { getString(it.stringRes) },
    ) { newSchedule ->
      when {
        newSchedule == BackupExportSchedule.OFF -> {
          // Nothing to write to, so no folder is needed.
          viewModel.saveExportBackupScheduleOff()
          showSnack(MessageEvent.Info(newSchedule.confirmationStringRes))
        }

        viewModel.isWebDavTarget() -> {
          // The destination is the configured server URL - there is no folder
          // to pick, so skip the picker entirely.
          viewModel.saveExportBackupSchedule(newSchedule)
          showSnack(MessageEvent.Info(newSchedule.confirmationStringRes))
        }

        else -> {
          selectedSchedule = newSchedule
          createFolderContract.launch(null)
        }
      }
    }
}
