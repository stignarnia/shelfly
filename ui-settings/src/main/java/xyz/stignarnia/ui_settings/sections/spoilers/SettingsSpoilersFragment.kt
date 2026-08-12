package xyz.stignarnia.ui_settings.sections.spoilers

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import xyz.stignarnia.ui_base.BaseFragment
import xyz.stignarnia.ui_base.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.ui_base.utilities.extensions.navigateToSafe
import xyz.stignarnia.ui_base.utilities.extensions.onClick
import xyz.stignarnia.ui_base.utilities.extensions.visibleIf
import xyz.stignarnia.ui_base.utilities.viewBinding
import xyz.stignarnia.ui_settings.R
import xyz.stignarnia.ui_settings.databinding.FragmentSettingsSpoilersBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsSpoilersFragment : BaseFragment<SettingsSpoilersViewModel>(R.layout.fragment_settings_spoilers) {

  override val navigationId = R.id.settingsFragment

  override val viewModel by viewModels<SettingsSpoilersViewModel>()
  private val binding by viewBinding(FragmentSettingsSpoilersBinding::bind)

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
      settingsSpoilersShows.onClick {
        navigateToSafe(R.id.actionSettingsFragmentToSpoilersShows)
      }
      settingsSpoilersMovies.onClick {
        navigateToSafe(R.id.actionSettingsFragmentToSpoilersMovies)
      }
      settingsSpoilersEpisodes.onClick {
        navigateToSafe(R.id.actionSettingsFragmentToSpoilersEpisodes)
      }
      settingsSpoilersTapToReveal.onClick {
        viewModel.setTapToReveal(!settingsSpoilersTapToRevealSwitch.isChecked)
      }
    }
  }

  private fun render(uiState: SettingsSpoilersUiState) {
    uiState.run {
      with(binding) {
        settingsSpoilersShowsCheck.visibleIf(hasShowsSettingActive)
        settingsSpoilersMoviesCheck.visibleIf(hasMoviesSettingActive)
        settingsSpoilersEpisodesCheck.visibleIf(hasEpisodesSettingActive)
        settingsSpoilersTapToRevealSwitch.isChecked = isTapToReveal
      }
    }
  }

  fun refreshSettings() = viewModel.loadSettings()
}
