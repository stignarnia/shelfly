package xyz.stignarnia.uiSettings.sections.apikeys

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiSettings.R
import xyz.stignarnia.uiSettings.databinding.FragmentSettingsApiKeysBinding
import xyz.stignarnia.uiSettings.databinding.ViewApiKeyInputBinding

@AndroidEntryPoint
class SettingsApiKeysFragment : BaseFragment<SettingsApiKeysViewModel>(R.layout.fragment_settings_api_keys) {
  override val viewModel by viewModels<SettingsApiKeysViewModel>()
  private val binding by viewBinding(FragmentSettingsApiKeysBinding::bind)

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
      settingsTmdbApiKey.onClick {
        showKeyDialog(
          titleResId = R.string.textSettingsTmdbApiKeyTitle,
          messageResId = R.string.textSettingsTmdbApiKeyDialogMessage,
          currentKey = viewModel.uiState.value.tmdbApiKey,
          onSave = { viewModel.setTmdbApiKey(it) },
        )
      }
      settingsOmdbApiKey.onClick {
        showKeyDialog(
          titleResId = R.string.textSettingsOmdbApiKeyTitle,
          messageResId = R.string.textSettingsOmdbApiKeyDialogMessage,
          currentKey = viewModel.uiState.value.omdbApiKey,
          onSave = { viewModel.setOmdbApiKey(it) },
        )
      }
    }
  }

  private fun showKeyDialog(
    titleResId: Int,
    messageResId: Int,
    currentKey: String,
    onSave: (String) -> Unit,
  ) {
    val inputBinding = ViewApiKeyInputBinding.inflate(LayoutInflater.from(requireContext()))
    inputBinding.apiKeyInput.setText(currentKey)

    modal()
      .setTitle(titleResId)
      .setMessage(messageResId)
      .setView(inputBinding.root)
      .setPositiveButton(R.string.textOk) { modal ->
        onSave(
          inputBinding.apiKeyInput.text
            ?.toString()
            .orEmpty(),
        )
        modal.dismiss()
      }.setNegativeButton(R.string.textCancel)
      .show()
  }

  private fun render(uiState: SettingsApiKeysUiState) {
    with(binding) {
      settingsTmdbApiKeyValue.text = mask(uiState.tmdbApiKey)
      settingsOmdbApiKeyValue.text = mask(uiState.omdbApiKey)
    }
  }

  /**
   * Shows enough of the key to recognise which one is configured, without putting the whole secret on screen.
   */
  private fun mask(key: String): String {
    if (key.isBlank()) {
      return getString(R.string.textSettingsApiKeyNotSet)
    }
    if (key.length <= 4) {
      return "****"
    }
    return "****" + key.takeLast(4)
  }
}
