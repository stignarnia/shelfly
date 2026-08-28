package xyz.stignarnia.shelfly.ui.main.cases

import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.common.Mode
import xyz.stignarnia.common.Mode.MOVIES
import xyz.stignarnia.common.Mode.SHOWS
import xyz.stignarnia.repository.settings.SettingsRepository
import javax.inject.Inject

@ViewModelScoped
class MainModesCase
  @Inject
  constructor(
    private val settingsRepository: SettingsRepository,
  ) {
    fun setMode(mode: Mode) {
      settingsRepository.mode = mode
    }

    fun getMode(): Mode {
      val isMoviesEnabled = settingsRepository.isMoviesEnabled
      val isMovies = settingsRepository.mode == MOVIES
      return if (isMoviesEnabled && isMovies) MOVIES else SHOWS
    }
  }
