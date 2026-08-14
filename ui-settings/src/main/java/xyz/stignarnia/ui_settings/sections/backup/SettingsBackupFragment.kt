package xyz.stignarnia.ui_settings.sections.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.ui_base.BaseFragment
import xyz.stignarnia.ui_base.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.visibleIf
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_model.BackupTarget
import xyz.stignarnia.ui_settings.R
import xyz.stignarnia.ui_settings.databinding.FragmentSettingsBackupBinding
import xyz.stignarnia.ui_settings.databinding.ViewWebdavInputBinding

@AndroidEntryPoint
class SettingsBackupFragment : BaseFragment<SettingsBackupViewModel>(R.layout.fragment_settings_backup) {

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
    }
  }

  private fun render(uiState: SettingsBackupUiState) {
    with(binding) {
      settingsBackupWebDavValue.text = when {
        uiState.isWebDavConfigured -> uiState.webDavUrl
        else -> getString(R.string.textSettingsWebDavNotConfigured)
      }
      settingsBackupTargetValue.setText(uiState.backupTarget.displayName())
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
      webdavTestButton.onClick {
        viewModel.testConnection(
          url = webdavUrlInput.text?.toString().orEmpty(),
          username = webdavUsernameInput.text?.toString().orEmpty(),
          password = webdavPasswordInput.text?.toString().orEmpty(),
        )
      }
    }

    // The stored password is never shown. An empty field means "leave it as it
    // is", so re-saving the URL does not silently wipe working credentials.
    if (state.hasWebDavPassword) {
      inputBinding.webdavPasswordInputLayout.hint = getString(R.string.textSettingsWebDavPasswordSetHint)
    }

    launchAndRepeatStarted(
      {
        viewModel.uiState.collect { uiState ->
          with(inputBinding) {
            webdavTestButton.isEnabled = uiState.connectionTest != ConnectionTest.Testing
            webdavTestResult.visibleIf(uiState.connectionTest != ConnectionTest.Idle)
            webdavTestResult.text = when (val test = uiState.connectionTest) {
              is ConnectionTest.Idle -> ""
              is ConnectionTest.Testing -> getString(R.string.textSettingsWebDavTesting)
              is ConnectionTest.Succeeded -> getString(R.string.textSettingsWebDavConnected)
              is ConnectionTest.Failed -> getString(test.reason)
            }
          }
        }
      },
    )

    MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialog)
      .setTitle(R.string.textSettingsWebDavTitle)
      .setMessage(R.string.textSettingsWebDavDialogMessage)
      .setView(inputBinding.root)
      .setPositiveButton(R.string.textOk) { _, _ ->
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
      }.setNegativeButton(R.string.textCancel) { _, _ -> }
      .setOnDismissListener { viewModel.clearConnectionTest() }
      .show()
  }

  private fun showTargetDialog() {
    val options = BackupTarget.entries
    val current = viewModel.uiState.value.backupTarget
    val labels = options.map { getString(it.displayName()) }.toTypedArray()

    MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialog)
      .setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_dialog))
      .setSingleChoiceItems(labels, options.indexOf(current)) { dialog, index ->
        viewModel.setBackupTarget(options[index])
        dialog.dismiss()
      }.show()
  }

  private fun BackupTarget.displayName() =
    when (this) {
      BackupTarget.LOCAL_FOLDER -> R.string.textSettingsBackupTargetLocal
      BackupTarget.WEBDAV -> R.string.textSettingsBackupTargetWebDav
    }
}
