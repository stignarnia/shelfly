package xyz.stignarnia.uiSettings.sections.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.utilities.AndroidVersion
import xyz.stignarnia.uiBase.utilities.events.Event
import xyz.stignarnia.uiBase.utilities.events.MessageEvent
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.openNotificationSettings
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiModel.NotificationDelay
import xyz.stignarnia.uiModel.Settings
import xyz.stignarnia.uiSettings.R
import xyz.stignarnia.uiSettings.databinding.FragmentSettingsNotificationsBinding
import xyz.stignarnia.uiSettings.sections.notifications.SettingsNotificationsUiEvent.NotificationsBlocked
import xyz.stignarnia.uiSettings.sections.notifications.views.NotificationsRationaleView

@AndroidEntryPoint
class SettingsNotificationsFragment :
  BaseFragment<SettingsNotificationsViewModel>(R.layout.fragment_settings_notifications) {
  override val viewModel by viewModels<SettingsNotificationsViewModel>()
  private val binding by viewBinding(FragmentSettingsNotificationsBinding::bind)

  /**
   * Set when a request goes out without a rationale first.
   *
   * shouldShowRequestPermissionRationale answers false both before the first ask and after a permanent refusal, so the two are told apart by what comes back: a refusal here means the system never prompted, and the only way forward is the settings screen.
   */
  private var requestedWithoutRationale = false

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    launchAndRepeatStarted(
      { viewModel.uiState.collect { render(it) } },
      { viewModel.eventFlow.collect { handleEvent(it) } },
      doAfterLaunch = { viewModel.loadSettings(requireAppContext()) },
    )
  }

  private fun setupView() {
    with(binding) {
      settingsShowsNotifications.onClick {
        viewModel.enableNotifications(!settingsShowsNotificationsSwitch.isChecked, requireAppContext())
      }
    }
  }

  private fun showWhenToNotifyDialog(settings: Settings) =
    showSingleChoiceModal(
      options = NotificationDelay.entries,
      selected = settings.episodesNotificationsDelay,
      label = { getString(it.stringRes) },
    ) { viewModel.setWhenToNotify(it) }

  private fun render(uiState: SettingsNotificationsUiState) {
    uiState.run {
      settings?.let {
        renderSettings(it)
      }
    }
  }

  private fun renderSettings(settings: Settings) {
    with(binding) {
      settingsShowsNotificationsSwitch.isChecked = settings.episodesNotificationsEnabled
      settingsWhenToNotifyValue.run {
        setText(settings.episodesNotificationsDelay.stringRes)
        settingsWhenToNotify.onClick { showWhenToNotifyDialog(settings) }
      }
    }
  }

  private fun handleEvent(event: Event<*>) {
    when (event) {
      is NotificationsBlocked -> onNotificationsBlocked()
    }
  }

  /**
   * The switch was turned on while the system has notifications off.
   *
   * Two different states look the same from the switch: a permission that has not been granted, and notifications the user has turned off in system settings.
   * Only the first is a permission the app can ask for, and it only exists from API 33 - so anywhere else, sending the user to settings is the one thing that can actually help.
   * Asking regardless is what made this loop: the request returns granted immediately, the app tries to enable again, the system still says notifications are off, and round it goes.
   */
  private fun onNotificationsBlocked() {
    if (!AndroidVersion.isAtLeastAndroid13) {
      openSystemNotificationSettings()
      return
    }
    askForNotificationsPermission()
  }

  /**
   * The half of [onNotificationsBlocked] that only exists from API 33.
   *
   * Split out and annotated rather than guarded inline so the requirement is part of the signature: POST_NOTIFICATIONS is an API 33 constant, and RequiresApi is what makes every caller prove it checked.
   */
  @RequiresApi(TIRAMISU)
  private fun askForNotificationsPermission() {
    val isAlreadyGranted =
      ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED

    when {
      isAlreadyGranted -> {
        openSystemNotificationSettings()
      }

      shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
        showNotificationsRationaleDialog()
      }

      else -> {
        requestedWithoutRationale = true
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  private fun openSystemNotificationSettings() {
    if (!openNotificationSettings()) {
      showSnack(MessageEvent.Error(R.string.errorNotificationSettingsUnavailable))
    }
  }

  @RequiresApi(TIRAMISU)
  private fun showNotificationsRationaleDialog() {
    val context = requireContext()
    val view = NotificationsRationaleView(context)
    modal()
      .setView(view)
      .setPositiveButton(R.string.textYes) { modal ->
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        modal.dismiss()
      }.setNegativeButton(R.string.textCancel)
      .show()
  }

  private val requestPermissionLauncher =
    registerForActivityResult(RequestPermission()) { isGranted ->
      when {
        isGranted -> {
          viewModel.enableNotifications(true, requireAppContext())
        }

        requestedWithoutRationale -> {
          requestedWithoutRationale = false
          openSystemNotificationSettings()
        }
      }
    }
}
