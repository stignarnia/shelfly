package xyz.stignarnia.ui_settings.sections.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.ui_base.BaseFragment
import xyz.stignarnia.ui_base.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.visibleIf
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.repository.settings.SettingsWebDavRepository
import xyz.stignarnia.ui_model.BackupTarget
import xyz.stignarnia.ui_settings.R
import xyz.stignarnia.ui_settings.databinding.FragmentSettingsBackupBinding
import xyz.stignarnia.ui_settings.databinding.ViewRetentionInputBinding
import xyz.stignarnia.ui_settings.databinding.ViewWebdavInputBinding

@AndroidEntryPoint
class SettingsBackupFragment : BaseFragment<SettingsBackupViewModel>(R.layout.fragment_settings_backup) {

  companion object {
    /** Matches the argument declared on settingsFragment in the navigation graph. */
    const val ARG_OPEN_WEB_DAV = "openWebDav"
  }

  override val viewModel by viewModels<SettingsBackupViewModel>()
  private val binding by viewBinding(FragmentSettingsBackupBinding::bind)

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()

    launchAndRepeatStarted(
      { viewModel.uiState.collect { render(it) } },
    )

    viewModel.refresh()
    openWebDavDialogIfRequested()
  }

  /**
   * The welcome flow sends the user here to set sync up, so open the form for
   * them instead of leaving it one more tap away. The flag is cleared once
   * consumed, otherwise the dialog would come back on every rotation.
   */
  private fun openWebDavDialogIfRequested() {
    val arguments = parentFragment?.arguments ?: return
    if (!arguments.getBoolean(ARG_OPEN_WEB_DAV, false)) return
    arguments.putBoolean(ARG_OPEN_WEB_DAV, false)
    showWebDavDialog()
  }

  private fun setupView() {
    with(binding) {
      settingsBackupExport.onClick {
        navigateTo(R.id.actionSettingsFragmentToBackupExport)
      }
      settingsBackupImport.onClick {
        navigateTo(R.id.actionSettingsFragmentToBackupImport)
      }
      settingsBackupWebDav.onClick { showWebDavDialog() }
      settingsBackupTarget.onClick { showTargetDialog() }
      settingsBackupRetention.onClick { showRetentionDialog() }
    }
  }

  private fun render(uiState: SettingsBackupUiState) {
    with(binding) {
      settingsBackupWebDavValue.text = when {
        uiState.isWebDavConfigured -> uiState.webDavUrl
        else -> getString(R.string.textSettingsWebDavNotConfigured)
      }
      settingsBackupTargetValue.setText(uiState.backupTarget.displayName())
      settingsBackupRetentionValue.text = when (uiState.backupRetention) {
        SettingsWebDavRepository.RETENTION_KEEP_ALL -> getString(R.string.textSettingsBackupRetentionKeepAll)
        else -> getString(R.string.textSettingsBackupRetentionValue, uiState.backupRetention)
      }
      // Choosing a destination is meaningless with nowhere to send it.
      settingsBackupTarget.visibleIf(uiState.isWebDavConfigured)
    }
  }

  private fun showWebDavDialog() {
    val state = viewModel.uiState.value
    val inputBinding = ViewWebdavInputBinding.inflate(LayoutInflater.from(requireContext()))

    with(inputBinding) {
      webdavUrlInput.setText(state.webDavUrl)
      webdavUsernameInput.setText(state.webDavUsername)
    }

    // The stored password is never shown. An empty field means "leave it as it
    // is", so re-saving the URL does not silently wipe working credentials.
    if (state.hasWebDavPassword) {
      inputBinding.webdavPasswordInputLayout.hint = getString(R.string.textSettingsWebDavPasswordSetHint)
    }

    val modal = modal()
      .setTitle(R.string.textSettingsWebDavTitle)
      .setMessage(R.string.textSettingsWebDavDialogMessage)
      .setView(inputBinding.root)
      .setNeutralButton(R.string.textSettingsWebDavTest) {
        viewModel.testConnection(
          url = inputBinding.webdavUrlInput.text
            ?.toString()
            .orEmpty(),
          username = inputBinding.webdavUsernameInput.text
            ?.toString()
            .orEmpty(),
          password = inputBinding.webdavPasswordInput.text
            ?.toString()
            .orEmpty(),
        )
      }.setPositiveButton(R.string.textOk) { modal ->
        viewModel.saveWebDav(
          url = inputBinding.webdavUrlInput.text
            ?.toString()
            .orEmpty(),
          username = inputBinding.webdavUsernameInput.text
            ?.toString()
            .orEmpty(),
          password = inputBinding.webdavPasswordInput.text
            ?.toString()
            .orEmpty(),
        )
        modal.dismiss()
      }.setNegativeButton(R.string.textCancel)
      .setOnDismiss { viewModel.clearConnectionTest() }
      .show()

    launchAndRepeatStarted(
      {
        viewModel.uiState.collect { uiState ->
          modal.setNeutralButtonEnabled(uiState.connectionTest != ConnectionTest.Testing)
          inputBinding.webdavTestResult.text = when (val test = uiState.connectionTest) {
            is ConnectionTest.Idle -> ""
            is ConnectionTest.Testing -> getString(R.string.textSettingsWebDavTesting)
            is ConnectionTest.Succeeded -> getString(R.string.textSettingsWebDavConnected)
            is ConnectionTest.Failed -> getString(test.reason)
          }
        }
      },
    )
  }

  private fun showRetentionDialog() {
    val current = viewModel.uiState.value.backupRetention
    val inputBinding = ViewRetentionInputBinding.inflate(LayoutInflater.from(requireContext()))
    inputBinding.retentionInput.setText(current.toString())

    modal()
      .setTitle(R.string.textSettingsBackupRetentionTitle)
      .setMessage(R.string.textSettingsBackupRetentionDescription)
      .setView(inputBinding.root)
      // A bad value is rejected without dismissing, so what was typed survives.
      .setPositiveButton(R.string.textOk) { modal ->
        val count = inputBinding.retentionInput.text
          ?.toString()
          ?.trim()
          ?.toIntOrNull()
        if (count == null || count < 0) {
          inputBinding.retentionInputLayout.error = getString(R.string.textSettingsBackupRetentionInvalid)
          return@setPositiveButton
        }
        viewModel.setBackupRetention(count)
        modal.dismiss()
      }.setNegativeButton(R.string.textCancel)
      .show()
  }

  private fun showTargetDialog() =
    showSingleChoiceModal(
      options = BackupTarget.entries,
      selected = viewModel.uiState.value.backupTarget,
      label = { getString(it.displayName()) },
    ) { viewModel.setBackupTarget(it) }

  private fun BackupTarget.displayName() =
    when (this) {
      BackupTarget.LOCAL_FOLDER -> R.string.textSettingsBackupTargetLocal
      BackupTarget.WEBDAV -> R.string.textSettingsBackupTargetWebDav
    }
}
