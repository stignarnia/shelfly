package xyz.stignarnia.uiBackup.features.imports

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import xyz.stignarnia.uiBackup.BackupException
import xyz.stignarnia.uiBackup.R
import xyz.stignarnia.uiBackup.databinding.FragmentBackupImportBinding
import xyz.stignarnia.uiBackup.describe
import xyz.stignarnia.uiBackup.features.export.cases.ReadBackupJsonFromFileUseCase
import xyz.stignarnia.uiBackup.features.imports.model.BackupImportStatus.Idle
import xyz.stignarnia.uiBackup.features.imports.model.BackupImportStatus.Importing
import xyz.stignarnia.uiBackup.features.imports.model.BackupImportStatus.Initializing
import xyz.stignarnia.uiBackup.features.imports.model.WebDavBackups
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.utilities.SnackbarHost
import xyz.stignarnia.uiBase.utilities.events.MessageEvent
import xyz.stignarnia.uiBase.utilities.events.MessageEvent.Error
import xyz.stignarnia.uiBase.utilities.extensions.doOnApplyWindowInsets
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.navigateBack
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.showErrorSnackbar
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.viewBinding
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@AndroidEntryPoint
class BackupImportFragment : BaseFragment<BackupImportViewModel>(R.layout.fragment_backup_import) {
  companion object {
    /** Matches the argument declared on backupImportFragment in the navigation graph. */
    const val ARG_IMPORT_LATEST_WEB_DAV = "importLatestWebDav"
  }

  @Inject
  lateinit var readBackupJsonFromFileUseCase: ReadBackupJsonFromFileUseCase

  override val viewModel by viewModels<BackupImportViewModel>()
  private val binding by viewBinding(FragmentBackupImportBinding::bind)

  private val pickFileContract =
    registerForActivityResult(
      OpenDocument(),
    ) { uri ->
      uri?.let { readImportFile(it) }
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
    )

    activity?.window?.addFlags(FLAG_KEEP_SCREEN_ON)
    importLatestWebDavIfRequested()
  }

  /**
   * The empty home screen's sync button sends the user here to restore the newest WebDAV backup, so start it for them instead of asking them to pick one.
   * The flag is cleared once consumed, otherwise the import would start again on every rotation.
   */
  private fun importLatestWebDavIfRequested() {
    val arguments = arguments ?: return
    if (!arguments.getBoolean(ARG_IMPORT_LATEST_WEB_DAV, false)) return
    arguments.putBoolean(ARG_IMPORT_LATEST_WEB_DAV, false)
    if (viewModel.isWebDavConfigured()) viewModel.importLatestWebDavBackup()
  }

  private fun setupView() {
    with(binding) {
      toolbar.onClick { navigateBack() }
      importButton.onClick { openNewImport() }
      importWebDavButton.onClick { viewModel.loadWebDavBackups() }
      // Only an option once a server is configured in Settings.
      importWebDavButton.visibleIf(viewModel.isWebDavConfigured())
      showLastReportButton.onClick {
        if (findNavController().currentDestination?.id == R.id.backupImportFragment) {
          findNavController().navigate(R.id.actionBackupImportToShowLastReport)
        }
      }
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

  private fun openNewImport() {
    pickFileContract.launch(arrayOf("application/json"))
  }

  private fun readImportFile(uri: Uri) {
    readBackupJsonFromFileUseCase(requireContext(), uri).fold(
      onSuccess = { viewModel.runImport(it) },
      onFailure = {
        showErrorSnack(BackupException(R.string.textBackupErrorRead, cause = it))
        Timber.e(it)
      },
    )
  }

  private fun showErrorSnack(error: Throwable) {
    if (error is CancellationException) {
      return
    }
    val host = (requireActivity() as SnackbarHost).provideSnackbarLayout()
    snackbar =
      host.showErrorSnackbar(
        message = error.describe(requireContext()),
      )
  }

  override fun onDestroyView() {
    if (viewModel.uiState.value.isImporting != Idle) {
      showSnack(Error(R.string.errorImportCancelled))
    } else {
      snackbar?.dismiss()
    }

    activity?.window?.clearFlags(FLAG_KEEP_SCREEN_ON)
    super.onDestroyView()
  }

  private fun render(uiState: BackupImportUiState) {
    uiState.run {
      with(binding) {
        when (val status = isImporting) {
          is Idle -> {
            importOverscroll.setRunningProgress(null)
          }

          is Initializing -> {
            importOverscroll.setRunning(true)
          }

          is Importing -> {
            val progressPercent = if (status.total > 0) (status.current * 100 / status.total) else null
            if (progressPercent != null) {
              importOverscroll.setRunningProgress(progressPercent)
            } else {
              importOverscroll.setRunning(true)
            }
          }
        }
        importButton.visibleIf(isImporting == Idle, gone = false)
        importButton.isEnabled = isImporting == Idle
        if (viewModel.isWebDavConfigured()) {
          importWebDavButton.visibleIf(isImporting == Idle, gone = false)
        }
        showLastReportButton.visibleIf(hasLastReport && isImporting == Idle, gone = true)
      }
      renderImportStatus(uiState)

      if (isSuccess) {
        viewModel.clearState()
        if (findNavController().currentDestination?.id == R.id.backupImportFragment) {
          findNavController().navigate(R.id.actionBackupImportToResult)
        }
      }

      if (isError != null) {
        showErrorSnack(isError)
        viewModel.clearState()
      }

      renderWebDavBackups(webDavBackups)
    }
  }

  private fun renderWebDavBackups(backups: WebDavBackups) {
    with(binding) {
      importWebDavButton.isEnabled = backups !is WebDavBackups.Loading
    }
    if (backups !is WebDavBackups.Loaded) return

    viewModel.clearWebDavBackups()
    if (backups.backups.isEmpty()) {
      showSnack(MessageEvent.Info(R.string.textBackupWebDavNoBackups))
      return
    }

    modal()
      .setTitle(R.string.textBackupWebDavPick)
      .setItems(backups.backups.map { it.label }) { index ->
        viewModel.runWebDavImport(backups.backups[index].fileName)
      }.setNegativeButton(R.string.textCancel)
      .show()
  }

  private fun renderImportStatus(uiState: BackupImportUiState) {
    with(binding) {
      statusText.visibleIf(uiState.isImporting != Idle)
      // The import runs in this screen's scope, so leaving the app lets the system reclaim it and cancel the import midway.
      stayInAppText.visibleIf(uiState.isImporting != Idle)
      statusText.text =
        when (val status = uiState.isImporting) {
          is Idle -> {
            ""
          }

          is Initializing -> {
            getString(R.string.textBackupImportInitializing)
          }

          is Importing -> {
            buildString {
              append(status.title)
              append("\n")
              append(status.current)
              append("/")
              append(status.total)
            }
          }
        }
    }
  }
}
