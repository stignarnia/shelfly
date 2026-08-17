package xyz.stignarnia.ui_settings.sections.misc

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import xyz.stignarnia.common.Config
import xyz.stignarnia.ui_base.BaseFragment
import xyz.stignarnia.ui_base.utilities.events.MessageEvent
import xyz.stignarnia.ui_base.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.openWebUrl
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_settings.BuildConfig
import xyz.stignarnia.ui_settings.R
import xyz.stignarnia.ui_settings.databinding.FragmentSettingsMiscBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsMiscFragment : BaseFragment<SettingsMiscViewModel>(R.layout.fragment_settings_misc) {

  override val viewModel by viewModels<SettingsMiscViewModel>()
  private val binding by viewBinding(FragmentSettingsMiscBinding::bind)

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    launchAndRepeatStarted(
      { viewModel.uiState.collect { render(it) } },
      { viewModel.messageFlow.collect { showSnack(it) } },
    )
  }

  private fun setupView() {
    with(binding) {
      settingsContactDevs.onClick { openWebLink(Config.GITHUB_ISSUES_URL) }
      settingsDeleteCache.onClick { viewModel.deleteImagesCache(requireAppContext()) }

      settingsTmdbIcon.onClick { openWebLink(Config.TMDB_URL) }
      settingsOmdbIcon.onClick { openWebLink(Config.OMDB_URL) }
      settingsJustWatchIcon.onClick { openWebLink(Config.JUST_WATCH_URL) }
    }
  }

  @SuppressLint("SetTextI18n")
  private fun render(uiState: SettingsMiscUiState) {
    uiState.run {
      with(binding) {
        settingsVersion.text = "v${BuildConfig.VER_NAME} (${BuildConfig.VER_CODE})"
      }
    }
  }

  private fun openWebLink(url: String) {
    openWebUrl(url) ?: showSnack(MessageEvent.Info(R.string.errorCouldNotFindApp))
  }
}
