package xyz.stignarnia.uiSettings.sections.widgets

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiSettings.R
import xyz.stignarnia.uiSettings.databinding.FragmentSettingsWidgetsBinding

@AndroidEntryPoint
class SettingsWidgetsFragment : BaseFragment<SettingsWidgetsViewModel>(R.layout.fragment_settings_widgets) {
  override val viewModel by viewModels<SettingsWidgetsViewModel>()
  private val binding by viewBinding(FragmentSettingsWidgetsBinding::bind)

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    launchAndRepeatStarted(
      { viewModel.uiState.collect { render(it) } },
      doAfterLaunch = { viewModel.loadSettings() },
    )
  }

  private fun setupView() {
    with(binding) {
      settingsWidgetsLabels.onClick {
        viewModel.enableWidgetsTitles(!settingsWidgetsLabelsSwitch.isChecked, requireAppContext())
      }
    }
  }

  private fun render(uiState: SettingsWidgetsUiState) {
    uiState.settings?.let {
      binding.settingsWidgetsLabelsSwitch.isChecked = it.widgetsShowLabel
    }
  }
}
