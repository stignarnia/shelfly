package xyz.stignarnia.uiSettings.sections.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import xyz.stignarnia.common.extensions.dateFromMillis
import xyz.stignarnia.common.extensions.toLocalZone
import xyz.stignarnia.repository.settings.SettingsWebDavRepository
import xyz.stignarnia.uiBackup.features.sync.model.SyncDeviceInfo
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.dates.DateFormatProvider
import xyz.stignarnia.uiBase.utilities.extensions.capitalizeWords
import xyz.stignarnia.uiBase.utilities.extensions.gone
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.visible
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiModel.BackupTarget
import xyz.stignarnia.uiSettings.R
import xyz.stignarnia.uiSettings.databinding.FragmentSettingsBackupBinding
import xyz.stignarnia.uiSettings.databinding.ItemSyncedDeviceBinding
import xyz.stignarnia.uiSettings.databinding.ViewDeviceNameInputBinding
import xyz.stignarnia.uiSettings.databinding.ViewRetentionInputBinding
import xyz.stignarnia.uiSettings.databinding.ViewSyncedDevicesBinding
import xyz.stignarnia.uiSettings.databinding.ViewWebdavInputBinding
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class SettingsBackupFragment : BaseFragment<SettingsBackupViewModel>(R.layout.fragment_settings_backup) {
  companion object {
    /** Matches the argument declared on settingsFragment in the navigation graph. */
    const val ARG_OPEN_WEB_DAV = "openWebDav"
    private const val DISABLED_ALPHA = 0.5F
  }

  override val viewModel by viewModels<SettingsBackupViewModel>()
  private val binding by viewBinding(FragmentSettingsBackupBinding::bind)

  @Inject
  lateinit var dateFormatProvider: DateFormatProvider

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
   * The welcome flow sends the user here to set sync up, so open the form for them instead of leaving it one more tap away.
   * The flag is cleared once consumed, otherwise the dialog would come back on every rotation.
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
      settingsBackupDeviceName.onClick {
        if (viewModel.uiState.value.isWebDavConfigured) {
          showDeviceNameDialog()
        }
      }
      settingsBackupDevices.onClick {
        if (viewModel.uiState.value.isWebDavConfigured) {
          showDevicesDialog()
        }
      }
    }
  }

  private fun render(uiState: SettingsBackupUiState) {
    with(binding) {
      settingsBackupWebDavValue.text =
        when {
          uiState.isWebDavConfigured -> uiState.webDavUrl
          else -> getString(R.string.textSettingsWebDavNotConfigured)
        }
      settingsBackupTargetValue.setText(uiState.backupTarget.displayName())
      settingsBackupRetentionValue.text =
        when (uiState.backupRetention) {
          SettingsWebDavRepository.RETENTION_KEEP_ALL -> {
            getString(R.string.textSettingsBackupRetentionKeepAll)
          }

          else -> {
            resources.getQuantityString(
              R.plurals.textSettingsBackupRetentionValue,
              uiState.backupRetention,
              uiState.backupRetention,
            )
          }
        }
      // Choosing a destination is meaningless with nowhere to send it.
      settingsBackupTarget.visibleIf(uiState.isWebDavConfigured)

      // Device Name Setting
      val configured = uiState.isWebDavConfigured
      settingsBackupDeviceName.isEnabled = configured
      settingsBackupDeviceName.alpha = if (configured) 1.0f else DISABLED_ALPHA
      settingsBackupDeviceNameValue.text =
        if (configured) {
          uiState.deviceName
        } else {
          getString(R.string.textSettingsBackupDeviceNameDisabled)
        }

      // Synced Devices Setting
      settingsBackupDevices.isEnabled = configured
      settingsBackupDevices.alpha = if (configured) 1.0f else DISABLED_ALPHA
    }
  }

  private fun showDeviceNameDialog() {
    val currentName = viewModel.uiState.value.deviceName
    val inputBinding = ViewDeviceNameInputBinding.inflate(LayoutInflater.from(requireContext()))
    inputBinding.deviceNameInput.setText(currentName)
    inputBinding.deviceNameInput.setSelection(currentName.length)

    modal()
      .setTitle(R.string.textSettingsBackupDeviceNameTitle)
      .setMessage(R.string.textSettingsBackupDeviceNameDialogMessage)
      .setView(inputBinding.root)
      .setNeutralButton(R.string.textSettingsBackupDeviceNameReset) {
        viewModel.setDeviceName("")
        val defaultName = viewModel.uiState.value.deviceName
        inputBinding.deviceNameInput.setText(defaultName)
        inputBinding.deviceNameInput.setSelection(defaultName.length)
      }.setPositiveButton(R.string.textOk) { modal ->
        val newName =
          inputBinding.deviceNameInput.text
            ?.toString()
            ?.trim()
            .orEmpty()
        viewModel.setDeviceName(newName)
        modal.dismiss()
      }.setNegativeButton(R.string.textCancel)
      .show()
  }

  private fun showDevicesDialog() {
    val dialogBinding = ViewSyncedDevicesBinding.inflate(LayoutInflater.from(requireContext()))
    modal()
      .setTitle(R.string.textSettingsBackupDevicesDialogTitle)
      .setMessage(R.string.textSettingsBackupDevicesDialogMessage)
      .setView(dialogBinding.root)
      .setPositiveButton(xyz.stignarnia.uiBase.R.string.textClose) { it.dismiss() }
      .show()

    viewModel.loadDevices()

    viewLifecycleOwner.lifecycleScope.launch {
      viewModel.uiState.collect { state ->
        when (val loadState = state.devicesLoadState) {
          is DevicesLoadState.Loading -> {
            dialogBinding.devicesLoading.visible()
            dialogBinding.devicesError.gone()
            dialogBinding.devicesEmpty.gone()
            dialogBinding.devicesList.gone()
          }

          is DevicesLoadState.Error -> {
            dialogBinding.devicesLoading.gone()
            dialogBinding.devicesError.visible()
            dialogBinding.devicesEmpty.gone()
            dialogBinding.devicesList.gone()
            dialogBinding.devicesError.setText(loadState.messageRes)
          }

          is DevicesLoadState.Loaded -> {
            dialogBinding.devicesLoading.gone()
            dialogBinding.devicesError.gone()
            if (loadState.devices.isEmpty()) {
              dialogBinding.devicesEmpty.visible()
              dialogBinding.devicesList.gone()
            } else {
              dialogBinding.devicesEmpty.gone()
              dialogBinding.devicesList.visible()
              renderDeviceList(dialogBinding, loadState.devices)
            }
          }

          is DevicesLoadState.Idle -> {
            dialogBinding.devicesLoading.gone()
            dialogBinding.devicesError.gone()
            dialogBinding.devicesEmpty.gone()
            dialogBinding.devicesList.gone()
          }
        }
      }
    }
  }

  private fun renderDeviceList(
    containerBinding: ViewSyncedDevicesBinding,
    devices: List<SyncDeviceInfo>,
  ) {
    containerBinding.devicesList.removeAllViews()
    val inflater = LayoutInflater.from(requireContext())

    devices.forEach { device ->
      val itemBinding = ItemSyncedDeviceBinding.inflate(inflater, containerBinding.devicesList, false)
      itemBinding.itemDeviceName.text =
        if (device.isCurrentDevice) {
          val thisDevice = getString(R.string.textSettingsBackupDevicesThisDevice)
          "${device.deviceName} ($thisDevice)"
        } else {
          device.deviceName
        }

      itemBinding.itemDeviceSubtitle.text =
        if (device.updatedAt > 0L) {
          getString(
            R.string.textSettingsBackupDevicesLastSynced,
            formatDate(device.updatedAt),
          )
        } else {
          getString(R.string.textSettingsBackupDevicesNeverSynced)
        }

      itemBinding.itemDeviceDelete.onClick {
        confirmDeleteDevice(device)
      }

      containerBinding.devicesList.addView(itemBinding.root)
    }
  }

  private fun confirmDeleteDevice(device: SyncDeviceInfo) {
    val message =
      if (device.isCurrentDevice) {
        getString(R.string.textSettingsBackupDevicesDeleteOwnMessage)
      } else {
        getString(R.string.textSettingsBackupDevicesDeleteMessage, device.deviceName)
      }

    modal()
      .setTitle(R.string.textSettingsBackupDevicesDeleteTitle)
      .setMessage(message)
      .setPositiveButton(xyz.stignarnia.uiBase.R.string.textRemove) { modal ->
        viewModel.deleteDevice(device.deviceId)
        modal.dismiss()
      }.setNegativeButton(R.string.textCancel)
      .show()
  }

  private fun formatDate(timestamp: Long): String {
    val date = dateFromMillis(timestamp).toLocalZone()
    return dateFormatProvider.loadFullHourFormat().format(date).capitalizeWords()
  }

  private fun showWebDavDialog() {
    val state = viewModel.uiState.value
    val inputBinding = ViewWebdavInputBinding.inflate(LayoutInflater.from(requireContext()))

    with(inputBinding) {
      webdavUrlInput.setText(state.webDavUrl)
      webdavUsernameInput.setText(state.webDavUsername)
    }

    // The stored password is never shown.
    // An empty field means "leave it as it is", so re-saving the URL does not silently wipe working credentials.
    if (state.hasWebDavPassword) {
      inputBinding.webdavPasswordInputLayout.hint = getString(R.string.textSettingsWebDavPasswordSetHint)
    }

    val modal =
      modal()
        .setTitle(R.string.textSettingsWebDavTitle)
        .setMessage(R.string.textSettingsWebDavDialogMessage)
        .setView(inputBinding.root)
        .setNeutralButton(R.string.textSettingsWebDavTest) {
          viewModel.testConnection(
            url =
              inputBinding.webdavUrlInput.text
                ?.toString()
                .orEmpty(),
            username =
              inputBinding.webdavUsernameInput.text
                ?.toString()
                .orEmpty(),
            password =
              inputBinding.webdavPasswordInput.text
                ?.toString()
                .orEmpty(),
          )
        }.setPositiveButton(R.string.textOk) { modal ->
          viewModel.saveWebDav(
            url =
              inputBinding.webdavUrlInput.text
                ?.toString()
                .orEmpty(),
            username =
              inputBinding.webdavUsernameInput.text
                ?.toString()
                .orEmpty(),
            password =
              inputBinding.webdavPasswordInput.text
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
          inputBinding.webdavTestResult.text =
            when (val test = uiState.connectionTest) {
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
    inputBinding.retentionInput.setText(String.format(Locale.getDefault(), "%d", current))

    modal()
      .setTitle(R.string.textSettingsBackupRetentionTitle)
      .setMessage(R.string.textSettingsBackupRetentionDescription)
      .setView(inputBinding.root)
      // A bad value is rejected without dismissing, so what was typed survives.
      .setPositiveButton(R.string.textOk) { modal ->
        val count =
          inputBinding.retentionInput.text
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
