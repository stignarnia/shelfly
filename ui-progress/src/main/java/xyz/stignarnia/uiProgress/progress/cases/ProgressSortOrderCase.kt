package xyz.stignarnia.uiProgress.progress.cases

import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import javax.inject.Inject

@ViewModelScoped
class ProgressSortOrderCase
  @Inject
  constructor(
    private val settingsRepository: SettingsRepository,
  ) {
    fun setSortOrder(
      sortOrder: SortOrder,
      sortType: SortType,
      newAtTop: Boolean,
    ) {
      settingsRepository.sorting.progressShowsSortOrder = sortOrder
      settingsRepository.sorting.progressShowsSortType = sortType
      settingsRepository.sorting.progressShowsNewAtTop = newAtTop
    }

    fun loadSortOrder() =
      Triple(
        settingsRepository.sorting.progressShowsSortOrder,
        settingsRepository.sorting.progressShowsSortType,
        settingsRepository.sorting.progressShowsNewAtTop,
      )
  }
