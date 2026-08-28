package xyz.stignarnia.uiMyMovies.hidden.cases

import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import javax.inject.Inject

@ViewModelScoped
class HiddenSortOrderCase
  @Inject
  constructor(
    private val settingsRepository: SettingsRepository,
  ) {
    fun setSortOrder(
      sortOrder: SortOrder,
      sortType: SortType,
    ) {
      settingsRepository.sorting.hiddenMoviesSortOrder = sortOrder
      settingsRepository.sorting.hiddenMoviesSortType = sortType
    }
  }
