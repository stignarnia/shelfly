package xyz.stignarnia.ui_my_shows.watchlist.cases

import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortType
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class WatchlistSortOrderCase @Inject constructor(
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
