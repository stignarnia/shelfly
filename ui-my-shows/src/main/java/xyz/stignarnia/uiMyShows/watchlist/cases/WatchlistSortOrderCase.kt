package xyz.stignarnia.uiMyShows.watchlist.cases

import dagger.hilt.android.scopes.ViewModelScoped
import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.uiModel.SortOrder
import xyz.stignarnia.uiModel.SortType
import javax.inject.Inject

@ViewModelScoped
class WatchlistSortOrderCase
  @Inject
  constructor(
    private val settingsRepository: SettingsRepository,
  ) {
    fun setSortOrder(
      sortOrder: SortOrder,
      sortType: SortType,
    ) {
      settingsRepository.sorting.watchlistShowsSortOrder = sortOrder
      settingsRepository.sorting.watchlistShowsSortType = sortType
    }
  }
