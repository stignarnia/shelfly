package xyz.stignarnia.uiProgress.progress.cases

import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiProgress.progress.recycler.ProgressListItem.Header.Type
import javax.inject.Inject

@ViewModelScoped
class ProgressHeadersCase
  @Inject
  constructor(
    private val settingsRepository: SettingsRepository,
  ) {
    fun toggleHeaderCollapsed(type: Type) {
      when (type) {
        Type.UPCOMING -> {
          val isCollapsed = settingsRepository.isProgressUpcomingCollapsed
          settingsRepository.isProgressUpcomingCollapsed = !isCollapsed
        }

        Type.ON_HOLD -> {
          val isCollapsed = settingsRepository.isProgressOnHoldCollapsed
          settingsRepository.isProgressOnHoldCollapsed = !isCollapsed
        }
      }
    }
  }
