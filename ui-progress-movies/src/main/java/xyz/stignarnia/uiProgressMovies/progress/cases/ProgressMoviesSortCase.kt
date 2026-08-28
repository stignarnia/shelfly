package xyz.stignarnia.uiProgressMovies.progress.cases

import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import javax.inject.Inject

@ViewModelScoped
class ProgressMoviesSortCase
  @Inject
  constructor(
    private val settingsRepository: SettingsRepository,
  ) {
    fun setSortOrder(
      sortOrder: SortOrder,
      sortType: SortType,
    ) {
      settingsRepository.sorting.progressMoviesSortOrder = sortOrder
      settingsRepository.sorting.progressMoviesSortType = sortType
    }
  }
