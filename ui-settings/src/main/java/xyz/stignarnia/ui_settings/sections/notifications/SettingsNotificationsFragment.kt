package xyz.stignarnia.ui_settings.sections.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
import android.provider.Settings.EXTRA_APP_PACKAGE
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.fragment.app.viewModels
import xyz.stignarnia.ui_base.BaseFragment
import xyz.stignarnia.ui_base.utilities.events.Event
import xyz.stignarnia.ui_base.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_model.NotificationDelay
import xyz.stignarnia.ui_model.Settings
import xyz.stignarnia.ui_settings.R
import xyz.stignarnia.ui_settings.databinding.FragmentSettingsNotificationsBinding
import xyz.stignarnia.ui_settings.sections.notifications.SettingsNotificationsUiEvent.RequestNotificationsPermission
import xyz.stignarnia.ui_settings.sections.notifications.views.NotificationsRationaleView
import dagger.hilt.android.AndroidEntryPoint

@SuppressLint("InlinedApi")
@AndroidEntryPoint
class SettingsNotificationsFragment :
  BaseFragment<SettingsNotificationsViewModel>(R.layout.fragment_settings_notifications) {

  override val viewModel by viewModels<SettingsNotificationsViewModel>()
  private val binding by viewBinding(FragmentSettingsNotificationsBinding::bind)

  private var notificationRationaleNotShown = false

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
    showSingleChoiceDialog(
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
      is RequestNotificationsPermission -> {
        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
          showNotificationsRationaleDialog()
        } else {
          notificationRationaleNotShown = true
          requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
      }
    }
  }

  private fun showNotificationsRationaleDialog() {
    val context = requireContext()
    val view = NotificationsRationaleView(context)
    dialog()
      .setView(view)
      .setPositiveButton(R.string.textYes) { _, _ ->
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }.setNegativeButton(R.string.textCancel) { _, _ -> }
      .show()
  }

  private val requestPermissionLauncher = registerForActivityResult(RequestPermission()) { isGranted ->
    if (isGranted) {
      viewModel.enableNotifications(true, requireAppContext())
    } else if (notificationRationaleNotShown) {
      val intent = Intent(ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(EXTRA_APP_PACKAGE, requireAppContext().packageName)
      }
      runCatching { startActivity(intent) }
      notificationRationaleNotShown = false
    }
  }
}
