package xyz.stignarnia.uiProgress.progress.cases

import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.repository.settings.SettingsRepository
import javax.inject.Inject

@ViewModelScoped
class ProgressFiltersCase
  @Inject
  constructor(
    private val settingsRepository: SettingsRepository,
  ) {
    fun setUpcomingFilter(isEnabled: Boolean) {
      settingsRepository.filters.progressShowsUpcoming = isEnabled
      if (isEnabled) {
        settingsRepository.filters.progressShowsOnHold = false
      }
    }

    fun setOnHoldFilter(isEnabled: Boolean) {
      settingsRepository.filters.progressShowsOnHold = isEnabled
      if (isEnabled) {
        settingsRepository.filters.progressShowsUpcoming = false
      }
    }
  }
