package xyz.stignarnia.ui_settings.sections.backup

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import xyz.stignarnia.ui_base.BaseFragment
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_settings.R
import xyz.stignarnia.ui_settings.databinding.FragmentSettingsBackupBinding
import dagger.hilt.android.AndroidEntryPoint

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
  }

  private fun setupView() {
    with(binding) {
      settingsBackupExport.onClick {
        navigateTo(R.id.actionSettingsFragmentToBackupExport)
      }
      settingsBackupImport.onClick {
        navigateTo(R.id.actionSettingsFragmentToBackupImport)
      }
    }
  }
}
