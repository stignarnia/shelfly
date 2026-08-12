package xyz.stignarnia.ui_settings.sections.widgets

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import xyz.stignarnia.ui_base.BaseFragment
import xyz.stignarnia.ui_base.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_settings.R
import xyz.stignarnia.ui_settings.databinding.FragmentSettingsWidgetsBinding
import dagger.hilt.android.AndroidEntryPoint

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
