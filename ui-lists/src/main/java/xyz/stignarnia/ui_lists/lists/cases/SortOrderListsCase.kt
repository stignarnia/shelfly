package xyz.stignarnia.ui_lists.lists.cases

import xyz.stignarnia.repository.settings.SettingsRepository
import xyz.stignarnia.ui_model.SortOrder
import xyz.stignarnia.ui_model.SortType
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class SortOrderListsCase @Inject constructor(
  private val settingsRepository: SettingsRepository,
) {

  fun setSortOrder(
    sortOrder: SortOrder,
    sortType: SortType,
  ) {
    settingsRepository.sorting.listsAllSortOrder = sortOrder
    settingsRepository.sorting.listsAllSortType = sortType
  }

  fun loadSortOrder() =
    Pair(
      settingsRepository.sorting.listsAllSortOrder,
      settingsRepository.sorting.listsAllSortType,
    )
}
