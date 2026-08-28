package xyz.stignarnia.uiSettings.sections.spoilers

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import xyz.stignarnia.uiBase.BaseFragment
import xyz.stignarnia.uiBase.utilities.extensions.launchAndRepeatStarted
import xyz.stignarnia.uiBase.utilities.extensions.navigateToSafe
import xyz.stignarnia.uiBase.utilities.extensions.onClick
import xyz.stignarnia.uiBase.utilities.extensions.visibleIf
import xyz.stignarnia.uiBase.utilities.viewBinding
import xyz.stignarnia.uiSettings.R
import xyz.stignarnia.uiSettings.databinding.FragmentSettingsSpoilersBinding

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
